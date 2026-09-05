package com.example.employeeapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * 部署別集計のテスト。
 *
 * 集計処理は「0件の行が結果から消える」「合計が合わない」で必ずバグる。
 * 手で数えて確認するのは現実的でないので、テストで固定する。
 */
@DataJpaTest
class EmployeeSummaryTest {

    @Autowired
    private EmployeeRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        // 営業部: 在籍2 / 退職1
        save("E0001", Department.SALES, EmploymentStatus.ACTIVE);
        save("E0002", Department.SALES, EmploymentStatus.ACTIVE);
        save("E0003", Department.SALES, EmploymentStatus.RETIRED);
        // 開発部: 在籍1 / 休職1
        save("E0004", Department.DEVELOPMENT, EmploymentStatus.ACTIVE);
        save("E0005", Department.DEVELOPMENT, EmploymentStatus.LEAVE);
        // 経理部・人事部・総務部: 0人
    }

    @Test
    @DisplayName("部署 × 在籍区分 の組み合わせごとに数えられる")
    void countByDepartmentAndStatus() {
        List<DepartmentStatusCount> counts = repository.countByDepartmentAndStatus();

        assertThat(counts)
                .contains(new DepartmentStatusCount(Department.SALES, EmploymentStatus.ACTIVE, 2L))
                .contains(new DepartmentStatusCount(Department.SALES, EmploymentStatus.RETIRED, 1L))
                .contains(new DepartmentStatusCount(Department.DEVELOPMENT, EmploymentStatus.LEAVE, 1L));
    }

    @Test
    @DisplayName("社員が0人の部署も、0名として結果に並ぶ")
    void zeroPersonDepartmentIsIncluded() {
        List<DepartmentSummary> summaries = summarize();

        // enum の全部署が並ぶ
        assertThat(summaries).hasSize(Department.values().length);

        DepartmentSummary hr = pick(summaries, Department.HR);
        assertThat(hr.active()).isZero();
        assertThat(hr.leave()).isZero();
        assertThat(hr.retired()).isZero();
        assertThat(hr.total()).isZero();
    }

    @Test
    @DisplayName("区分ごとの内訳が正しい")
    void breakdownIsCorrect() {
        DepartmentSummary sales = pick(summarize(), Department.SALES);

        assertThat(sales.active()).isEqualTo(2);
        assertThat(sales.leave()).isZero();
        assertThat(sales.retired()).isEqualTo(1);
    }

    @Test
    @DisplayName("合計は内訳の足し算と一致する")
    void totalMatchesBreakdown() {
        for (DepartmentSummary s : summarize()) {
            assertThat(s.total())
                    .as("%s の合計", s.department())
                    .isEqualTo(s.active() + s.leave() + s.retired());
        }
    }

    @Test
    @DisplayName("全部署の合計が、登録した社員数と一致する")
    void grandTotalMatchesRowCount() {
        long grandTotal = summarize().stream().mapToLong(DepartmentSummary::total).sum();

        assertThat(grandTotal).isEqualTo(repository.count());
    }

    @Test
    @DisplayName("構成比は母数が0でも例外にならず0を返す")
    void ratioWithZeroGrandTotal() {
        DepartmentSummary s = new DepartmentSummary(Department.SALES, 0, 0, 0);

        assertThat(s.ratio(0)).isZero();
    }

    @Test
    @DisplayName("構成比が正しく計算される")
    void ratioIsCalculated() {
        DepartmentSummary sales = pick(summarize(), Department.SALES);

        // 営業部3名 / 全体5名 = 60%
        assertThat(sales.ratio(5)).isEqualTo(60);
    }

    /** サービスを通さず、リポジトリの結果から同じ組み立てを行う */
    private List<DepartmentSummary> summarize() {
        return new EmployeeService(repository).summarizeByDepartment();
    }

    private DepartmentSummary pick(List<DepartmentSummary> list, Department department) {
        return list.stream()
                .filter(s -> s.department() == department)
                .findFirst()
                .orElseThrow();
    }

    private void save(String number, Department department, EmploymentStatus status) {
        Employee e = new Employee();
        e.setEmployeeNumber(number);
        e.setName("テスト " + number);
        e.setNameKana("テスト");
        e.setDepartment(department);
        e.setEmail(number.toLowerCase() + "@example.com");
        e.setHireDate(LocalDate.of(2020, 4, 1));
        e.setStatus(status);
        e.setNote("");
        repository.save(e);
    }
}
