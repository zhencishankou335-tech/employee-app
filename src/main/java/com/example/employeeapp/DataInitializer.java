package com.example.employeeapp;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.repository.EmployeeRepository;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 動作確認用の初期データを入れる。
 *
 * データが1件も無いときだけ実行するので、
 * 何度起動しても同じ社員が増えていくことはない。
 * 本番運用するアプリには入れない類のクラス。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository repository;

    public DataInitializer(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        save("E0001", "山田 太郎", "ヤマダ タロウ", Department.SALES,
                "yamada@example.com", LocalDate.of(2018, 4, 1), EmploymentStatus.ACTIVE, "");
        save("E0002", "佐藤 花子", "サトウ ハナコ", Department.DEVELOPMENT,
                "sato@example.com", LocalDate.of(2020, 10, 1), EmploymentStatus.ACTIVE, "");
        save("E0003", "鈴木 一郎", "スズキ イチロウ", Department.ACCOUNTING,
                "suzuki@example.com", LocalDate.of(2015, 4, 1), EmploymentStatus.ACTIVE, "");
        save("E0004", "田中 美咲", "タナカ ミサキ", Department.HR,
                "tanaka@example.com", LocalDate.of(2022, 4, 1), EmploymentStatus.LEAVE, "育児休業中");
        save("E0005", "高橋 健", "タカハシ ケン", Department.DEVELOPMENT,
                "takahashi@example.com", LocalDate.of(2019, 7, 16), EmploymentStatus.ACTIVE, "");
        save("E0006", "伊藤 由美", "イトウ ユミ", Department.GENERAL_AFFAIRS,
                "ito@example.com", LocalDate.of(2012, 4, 2), EmploymentStatus.ACTIVE, "");
        save("E0007", "渡辺 修", "ワタナベ オサム", Department.SALES,
                "watanabe@example.com", LocalDate.of(2016, 9, 1), EmploymentStatus.RETIRED, "2024年3月末退職");
        save("E0008", "小林 彩", "コバヤシ アヤ", Department.DEVELOPMENT,
                "kobayashi@example.com", LocalDate.of(2023, 4, 3), EmploymentStatus.ACTIVE, "");
        save("E0009", "加藤 大輔", "カトウ ダイスケ", Department.ACCOUNTING,
                "kato@example.com", LocalDate.of(2021, 1, 12), EmploymentStatus.ACTIVE, "");
        save("E0010", "吉田 千夏", "ヨシダ チナツ", Department.SALES,
                "yoshida@example.com", LocalDate.of(2024, 4, 1), EmploymentStatus.ACTIVE, "");
        save("E0011", "山本 翔", "ヤマモト ショウ", Department.DEVELOPMENT,
                "yamamoto@example.com", LocalDate.of(2017, 4, 1), EmploymentStatus.ACTIVE, "");
        save("E0012", "中村 恵", "ナカムラ メグミ", Department.HR,
                "nakamura@example.com", LocalDate.of(2014, 11, 4), EmploymentStatus.ACTIVE, "");
    }

    private void save(String number, String name, String kana, Department department,
                      String email, LocalDate hireDate, EmploymentStatus status, String note) {
        Employee e = new Employee();
        e.setEmployeeNumber(number);
        e.setName(name);
        e.setNameKana(kana);
        e.setDepartment(department);
        e.setEmail(email);
        e.setHireDate(hireDate);
        e.setStatus(status);
        e.setNote(note);
        repository.save(e);
    }
}
