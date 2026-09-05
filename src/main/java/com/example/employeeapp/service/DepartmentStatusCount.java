package com.example.employeeapp.service;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.EmploymentStatus;

/**
 * 「部署 × 在籍区分」ごとの件数。DBから返ってくる生の集計結果。
 *
 * record は「値を持つだけのクラス」を短く書くための書き方。
 * getter や equals を自動で作ってくれる。
 *
 * 件数は Long（long ではない）。JPQL の count() が Long を返すため、
 * long にするとコンストラクタが一致せず実行時に落ちる。
 */
public record DepartmentStatusCount(Department department,
                                    EmploymentStatus status,
                                    Long count) {
}
