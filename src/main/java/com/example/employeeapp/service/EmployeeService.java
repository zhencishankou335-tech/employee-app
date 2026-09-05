package com.example.employeeapp.service;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.repository.EmployeeRepository;
import com.example.employeeapp.web.EmployeeForm;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 業務ロジック担当。
 *
 * 「どういう条件なら登録してよいか」「削除は物理か論理か」といった
 * “会社のルール” をここに集める。画面（Controller）とDB（Repository）の間に
 * この層を挟むのが業務系の基本形。
 *
 * ここが薄いと、同じルールが複数の画面にコピーされて必ず食い違う。
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository repository;

    /** コンストラクタで受け取る（コンストラクタインジェクション） */
    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    /** 一覧検索（ページングあり） */
    public Page<Employee> search(String keyword, Department department,
                                 EmploymentStatus status, Pageable pageable) {
        return repository.search(normalize(keyword), department, status, pageable);
    }

    /** CSV出力用（ページングなし・全件） */
    public List<Employee> searchAll(String keyword, Department department,
                                    EmploymentStatus status) {
        return repository.searchAll(normalize(keyword), department, status);
    }

    /**
     * 部署ごとの人数を集計する。
     *
     * DBの group by は「該当する行がある組み合わせ」しか返さない。
     * 社員が0人の部署は結果から消えるが、集計表としてはそれでは困る
     * （「営業部が無い＝存在しない部署なのか、0人なのか」が読み手に分からない）。
     * そこで全部署を必ず並べ、該当が無ければ0を入れる。
     */
    public List<DepartmentSummary> summarizeByDepartment() {
        List<DepartmentStatusCount> counts = repository.countByDepartmentAndStatus();

        List<DepartmentSummary> summaries = new ArrayList<>();
        for (Department department : Department.values()) {
            summaries.add(new DepartmentSummary(
                    department,
                    countOf(counts, department, EmploymentStatus.ACTIVE),
                    countOf(counts, department, EmploymentStatus.LEAVE),
                    countOf(counts, department, EmploymentStatus.RETIRED)));
        }
        return summaries;
    }

    /** 集計結果から該当の組み合わせを取り出す。無ければ0 */
    private long countOf(List<DepartmentStatusCount> counts,
                         Department department, EmploymentStatus status) {
        return counts.stream()
                .filter(c -> c.department() == department && c.status() == status)
                .mapToLong(DepartmentStatusCount::count)
                .findFirst()
                .orElse(0L);
    }

    /** 1件取得。見つからなければ例外 */
    public Employee findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("社員が見つかりません（id=" + id + "）"));
    }

    /**
     * 新規登録。
     * 社員番号の重複はDBの unique 制約でも止まるが、
     * それだと利用者に「DBエラー」としか見えないので、ここで先に判定して
     * 日本語のメッセージを返せるようにしている。
     */
    @Transactional
    public Employee create(EmployeeForm form) {
        repository.findByEmployeeNumber(form.getEmployeeNumber())
                .ifPresent(existing -> {
                    throw new DuplicateEmployeeNumberException(form.getEmployeeNumber());
                });

        Employee employee = new Employee();
        applyFormTo(employee, form);
        return repository.save(employee);
    }

    /**
     * 更新。
     * 社員番号を他人のものに変更しようとしていないかを確認する。
     * 「自分自身と同じ番号」は当然OKなので、そこを除外して判定する。
     *
     * あわせて排他制御（楽観ロック）の判定もここでやる。
     */
    @Transactional
    public Employee update(Long id, EmployeeForm form) {
        Employee employee = findById(id);

        /*
         * 排他制御：画面を開いたときの版数と、今DBにある版数を突き合わせる。
         *
         * 食い違っていれば、編集画面を開いている間に誰かが保存したということ。
         * そのまま上書きすると相手の変更が消えるので、ここで止める。
         *
         * エンティティ側の @Version による検知（DBのUPDATE時に効く）もあるが、
         * それだけだと「同時刻に処理が重なったとき」しか捕まえられない。
         * 画面を開きっぱなしにして10分後に保存、という業務でよくある操作は
         * こちらの明示的な比較でないと検知できない。
         *
         * 社員番号の重複チェックを「アプリ側とDB側の両方」でやっているのと同じ考え方。
         */
        if (form.getVersion() == null || !form.getVersion().equals(employee.getVersion())) {
            throw new StaleEmployeeException(employee.getName());
        }

        Optional<Employee> sameNumber = repository.findByEmployeeNumber(form.getEmployeeNumber());
        if (sameNumber.isPresent() && !sameNumber.get().getId().equals(id)) {
            throw new DuplicateEmployeeNumberException(form.getEmployeeNumber());
        }

        applyFormTo(employee, form);
        return repository.save(employee);
    }

    /**
     * 削除。
     *
     * 実務では、過去データから参照される可能性があるため
     * 行を消さず在籍区分を「退職」に変える（論理削除）ことが多い。
     * ここでは練習として物理削除も用意しているが、既定は論理削除。
     */
    @Transactional
    public void retire(Long id) {
        Employee employee = findById(id);
        employee.setStatus(EmploymentStatus.RETIRED);
        repository.save(employee);
    }

    /** 物理削除（誤登録の取り消し用） */
    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    /** 画面の入力値をエンティティに移す */
    private void applyFormTo(Employee employee, EmployeeForm form) {
        employee.setEmployeeNumber(form.getEmployeeNumber().trim());
        employee.setName(form.getName().trim());
        employee.setNameKana(form.getNameKana().trim());
        employee.setDepartment(form.getDepartment());
        employee.setEmail(normalize(form.getEmail()));
        employee.setHireDate(form.getHireDate());
        employee.setStatus(form.getStatus());
        employee.setNote(normalize(form.getNote()));
    }

    /** null を空文字に寄せる。検索条件の分岐を1か所にまとめるため */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
