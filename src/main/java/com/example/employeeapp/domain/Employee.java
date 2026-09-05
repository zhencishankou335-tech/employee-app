package com.example.employeeapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社員（エンティティ）。
 *
 * このクラス1つが、データベースの EMPLOYEE テーブル1行に対応する。
 * フィールド1つが列1つ。JPA がクラスとテーブルを自動で結びつけてくれるので、
 * CREATE TABLE 文を手で書く必要がない。
 */
@Entity
@Table(name = "employee")
public class Employee {

    /** 主キー。業務上の意味を持たない連番（サロゲートキー） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 社員番号。人が見て識別するための業務キー。
     * unique = true でDB側にも重複禁止をかけている。
     * 「アプリ側のチェックだけ」にすると、同時に2人が登録したときにすり抜ける。
     */
    @Column(nullable = false, unique = true, length = 10)
    private String employeeNumber;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nameKana;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Department department;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentStatus status;

    @Column(length = 200)
    private String note;

    /**
     * 登録日時・更新日時。
     * 業務系では「いつ誰が触ったか」を残すのがほぼ必須。
     * 更新者IDまでは今回持たせていない（README の「入れていないもの」に記載）。
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 版数（楽観ロック用）。
     *
     * @Version を付けると、Hibernate が UPDATE 文に
     *   where id = ? and version = ?
     * を自動で足し、成功したら version を +1 する。
     * 他の人が先に更新していれば version が変わっているので、
     * 更新対象が0件になり ObjectOptimisticLockingFailureException が投げられる。
     *
     * 「先に保存した人の変更が、後から保存した人に黙って上書きされる」事故を防ぐための仕組み。
     * 値はアプリが触らない。Hibernate が管理する。
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /** INSERT の直前に自動で呼ばれる */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** UPDATE の直前に自動で呼ばれる */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- 以下は単なる getter / setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameKana() {
        return nameKana;
    }

    public void setNameKana(String nameKana) {
        this.nameKana = nameKana;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public EmploymentStatus getStatus() {
        return status;
    }

    public void setStatus(EmploymentStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    /**
     * 画面から戻ってきた版数を戻すためだけに使う。
     * 通常の業務処理からは呼ばない（値は Hibernate が管理する）。
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}
