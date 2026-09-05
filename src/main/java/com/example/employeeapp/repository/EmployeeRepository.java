package com.example.employeeapp.repository;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.service.DepartmentStatusCount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * データベースアクセス担当。
 *
 * インターフェースを書くだけで、Spring Data JPA が実装クラスを自動生成する。
 * findAll / findById / save / deleteById などは JpaRepository から継承される。
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** 社員番号の重複チェック用（新規登録時） */
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    /**
     * 一覧の検索。業務系の一覧画面はほぼこの形。
     *
     * 「検索条件が未入力なら、その条件では絞り込まない」を1本のJPQLで表現している。
     * keyword は空文字なら素通り、department / status は null なら素通り。
     */
    @Query("""
            select e from Employee e
            where (:keyword = ''
                   or e.name           like concat('%', :keyword, '%')
                   or e.nameKana       like concat('%', :keyword, '%')
                   or e.employeeNumber like concat('%', :keyword, '%'))
              and (:department is null or e.department = :department)
              and (:status     is null or e.status     = :status)
            """)
    Page<Employee> search(@Param("keyword") String keyword,
                          @Param("department") Department department,
                          @Param("status") EmploymentStatus status,
                          Pageable pageable);

    /**
     * CSV出力用。こちらはページングせず全件返す。
     * 画面表示とCSVで検索条件がズレると現場で必ず問い合わせになるので、条件は同じにしてある。
     */
    @Query("""
            select e from Employee e
            where (:keyword = ''
                   or e.name           like concat('%', :keyword, '%')
                   or e.nameKana       like concat('%', :keyword, '%')
                   or e.employeeNumber like concat('%', :keyword, '%'))
              and (:department is null or e.department = :department)
              and (:status     is null or e.status     = :status)
            order by e.employeeNumber
            """)
    List<Employee> searchAll(@Param("keyword") String keyword,
                             @Param("department") Department department,
                             @Param("status") EmploymentStatus status);

    /**
     * 部署 × 在籍区分 ごとの人数を数える。
     *
     * group by で集計するので、DBから返るのは「組み合わせが存在する分だけ」。
     * 社員が1人もいない部署はここに現れない。
     * 画面には全部署を並べたいので、その穴埋めはサービス層でやる。
     *
     * select new ... はJPQLの「コンストラクタ式」。
     * Object[] のまま受け取ると添字でアクセスすることになり、後から読めなくなる。
     */
    @Query("""
            select new com.example.employeeapp.service.DepartmentStatusCount(
                       e.department, e.status, count(e))
            from Employee e
            group by e.department, e.status
            """)
    List<DepartmentStatusCount> countByDepartmentAndStatus();
}
