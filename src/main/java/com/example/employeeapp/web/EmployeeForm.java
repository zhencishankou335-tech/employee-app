package com.example.employeeapp.web;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 登録・編集画面の入力を受け取る入れ物（フォームクラス）。
 *
 * なぜエンティティを直接使わないのか：
 *   画面から来る値は「まだ検証していない、信用できない値」だから。
 *   これをそのままDBのクラスに流し込むと、意図しない項目まで
 *   書き換えられる事故（マスアサインメント）が起きる。
 *   業務系ではフォームとエンティティを分けるのが定石。
 */
public class EmployeeForm {

    /** 更新時のみ値が入る。新規のときは null */
    private Long id;

    @NotBlank(message = "社員番号を入力してください")
    @Pattern(regexp = "^[A-Z][0-9]{4}$", message = "社員番号は英大文字1字＋数字4桁で入力してください（例：E0001）")
    private String employeeNumber;

    @NotBlank(message = "氏名を入力してください")
    @Size(max = 50, message = "氏名は50文字以内で入力してください")
    private String name;

    @NotBlank(message = "フリガナを入力してください")
    @Size(max = 50, message = "フリガナは50文字以内で入力してください")
    @Pattern(regexp = "^[ァ-ヶー　 ]+$", message = "フリガナは全角カタカナで入力してください")
    private String nameKana;

    @NotNull(message = "部署を選択してください")
    private Department department;

    @Email(message = "メールアドレスの形式が正しくありません")
    @Size(max = 100, message = "メールアドレスは100文字以内で入力してください")
    private String email;

    /**
     * 入社日。
     * DateTimeFormat が無いと "2024-04-01" という文字列を LocalDate に変換できない。
     */
    @NotNull(message = "入社日を入力してください")
    @PastOrPresent(message = "入社日に未来の日付は入力できません")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hireDate;

    @NotNull(message = "在籍区分を選択してください")
    private EmploymentStatus status;

    @Size(max = 200, message = "備考は200文字以内で入力してください")
    private String note;

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
}
