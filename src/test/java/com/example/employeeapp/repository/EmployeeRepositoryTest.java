package com.example.employeeapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 検索処理のテスト。
 *
 * @DataJpaTest を付けると、DB周りだけを読み込んだ軽い状態でテストできる。
 * 実行のたびにメモリ上のDBが作られ、各テストの後で自動的に巻き戻される。
 *
 * 業務系の一覧検索は「条件を組み合わせたときに正しく絞れるか」でバグが出やすく、
 * ここは手動テストだと組み合わせが多すぎて漏れる。だからテストコードで固定する。
 */
@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        save("E0001", "山田 太郎", "ヤマダ タロウ", Department.SALES, EmploymentStatus.ACTIVE);
        save("E0002", "佐藤 花子", "サトウ ハナコ", Department.DEVELOPMENT, EmploymentStatus.ACTIVE);
        save("E0003", "山田 次郎", "ヤマダ ジロウ", Department.DEVELOPMENT, EmploymentStatus.RETIRED);
        save("E0004", "鈴木 一郎", "スズキ イチロウ", Department.ACCOUNTING, EmploymentStatus.LEAVE);
    }

    @Test
    @DisplayName("条件を何も指定しなければ全件返る")
    void searchWithoutConditionReturnsAll() {
        Page<Employee> result = search("", null, null);
        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("氏名の部分一致で絞り込める")
    void searchByNameKeyword() {
        Page<Employee> result = search("山田", null, null);

        assertThat(result.getContent())
                .extracting(Employee::getEmployeeNumber)
                .containsExactly("E0001", "E0003");
    }

    @Test
    @DisplayName("フリガナでも社員番号でも同じ検索欄で引ける")
    void searchByKanaAndEmployeeNumber() {
        assertThat(search("サトウ", null, null).getTotalElements()).isEqualTo(1);
        assertThat(search("E0004", null, null).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("部署で絞り込める")
    void searchByDepartment() {
        Page<Employee> result = search("", Department.DEVELOPMENT, null);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("キーワードと部署と在籍区分を同時に指定するとAND条件になる")
    void searchByMultipleConditions() {
        Page<Employee> result = search("山田", Department.DEVELOPMENT, EmploymentStatus.RETIRED);

        assertThat(result.getContent())
                .extracting(Employee::getEmployeeNumber)
                .containsExactly("E0003");
    }

    @Test
    @DisplayName("該当なしのときは0件で、例外にはならない")
    void searchWithNoMatch() {
        Page<Employee> result = search("存在しない名前", null, null);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("CSV用の全件検索は、画面の検索と同じ条件で同じ結果になる")
    void searchAllMatchesPagedSearch() {
        Page<Employee> paged = search("", Department.DEVELOPMENT, null);
        List<Employee> all = repository.searchAll("", Department.DEVELOPMENT, null);

        assertThat(all).hasSize((int) paged.getTotalElements());
    }

    @Test
    @DisplayName("社員番号で1件取得できる")
    void findByEmployeeNumber() {
        assertThat(repository.findByEmployeeNumber("E0002")).isPresent();
        assertThat(repository.findByEmployeeNumber("E9999")).isEmpty();
    }

    private Page<Employee> search(String keyword, Department department, EmploymentStatus status) {
        return repository.search(keyword, department, status,
                PageRequest.of(0, 10, Sort.by("employeeNumber")));
    }

    private void save(String number, String name, String kana,
                      Department department, EmploymentStatus status) {
        Employee e = new Employee();
        e.setEmployeeNumber(number);
        e.setName(name);
        e.setNameKana(kana);
        e.setDepartment(department);
        e.setEmail(number.toLowerCase() + "@example.com");
        e.setHireDate(LocalDate.of(2020, 4, 1));
        e.setStatus(status);
        e.setNote("");
        repository.save(e);
    }
}
