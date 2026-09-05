package com.example.employeeapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.repository.EmployeeRepository;
import com.example.employeeapp.web.EmployeeForm;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * 排他制御（楽観ロック）のテスト。
 *
 * 【防ぎたい事故】
 *   AさんとBさんが同じ社員の編集画面を開く
 *   → Bさんが保存
 *   → Aさんが保存
 *   → Bさんの変更が黙って消える（ロストアップデート）
 *
 * この事故は、実際に起きても誰も気づかないのが最悪の点。
 * 「先月直したはずの部署がまた元に戻っている」という形で、
 * 何か月も経ってから発覚することがある。
 *
 * 手で再現するには2人が同時に操作する必要があり現実的でないので、
 * テストで固定する。
 */
@DataJpaTest
class EmployeeOptimisticLockTest {

    @Autowired
    private EmployeeRepository repository;

    @Test
    @DisplayName("新規登録すると版数は0から始まる")
    void versionStartsAtZero() {
        Employee saved = repository.saveAndFlush(newEmployee());

        assertThat(saved.getVersion()).isZero();
    }

    @Test
    @DisplayName("更新するたびに版数が1つ上がる")
    void versionIncrementsOnEachUpdate() {
        Employee saved = repository.saveAndFlush(newEmployee());
        EmployeeService service = new EmployeeService(repository);

        service.update(saved.getId(), formFor(saved, 0L, "1回目の更新"));
        repository.flush();
        assertThat(repository.findById(saved.getId()).orElseThrow().getVersion()).isEqualTo(1L);

        service.update(saved.getId(), formFor(saved, 1L, "2回目の更新"));
        repository.flush();
        assertThat(repository.findById(saved.getId()).orElseThrow().getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("古い版数のまま保存しようとすると、上書きせずに例外で止める")
    void staleVersionIsRejected() {
        Employee saved = repository.saveAndFlush(newEmployee());
        EmployeeService service = new EmployeeService(repository);

        // Bさんが先に保存した（版数 0 → 1）
        service.update(saved.getId(), formFor(saved, 0L, "Bさんの変更"));
        repository.flush();

        // Aさんは版数0のときに画面を開いたまま。そのまま保存しようとする。
        assertThatThrownBy(() ->
                service.update(saved.getId(), formFor(saved, 0L, "Aさんの変更")))
                .isInstanceOf(StaleEmployeeException.class)
                .hasMessageContaining("他の利用者によって更新されています");

        // Bさんの変更が残っていること（Aさんに上書きされていないこと）
        assertThat(repository.findById(saved.getId()).orElseThrow().getNote())
                .isEqualTo("Bさんの変更");
    }

    @Test
    @DisplayName("版数が送られてこなかった場合も、安全側に倒して拒否する")
    void missingVersionIsRejected() {
        Employee saved = repository.saveAndFlush(newEmployee());
        EmployeeService service = new EmployeeService(repository);

        // 画面の hidden 項目が欠けている＝どの時点のデータを見ていたか分からない。
        // 「たぶん最新だろう」で通してはいけない。
        assertThatThrownBy(() ->
                service.update(saved.getId(), formFor(saved, null, "版数なし")))
                .isInstanceOf(StaleEmployeeException.class);
    }

    private Employee newEmployee() {
        Employee e = new Employee();
        e.setEmployeeNumber("E0100");
        e.setName("松本 遥");
        e.setNameKana("マツモト ハルカ");
        e.setDepartment(Department.DEVELOPMENT);
        e.setEmail("matsumoto@example.com");
        e.setHireDate(LocalDate.of(2025, 4, 1));
        e.setStatus(EmploymentStatus.ACTIVE);
        e.setNote("");
        return e;
    }

    /** 編集画面から戻ってきた入力を模したフォーム */
    private EmployeeForm formFor(Employee e, Long version, String note) {
        EmployeeForm form = new EmployeeForm();
        form.setId(e.getId());
        form.setVersion(version);
        form.setEmployeeNumber(e.getEmployeeNumber());
        form.setName(e.getName());
        form.setNameKana(e.getNameKana());
        form.setDepartment(e.getDepartment());
        form.setEmail(e.getEmail());
        form.setHireDate(e.getHireDate());
        form.setStatus(e.getStatus());
        form.setNote(note);
        return form;
    }
}
