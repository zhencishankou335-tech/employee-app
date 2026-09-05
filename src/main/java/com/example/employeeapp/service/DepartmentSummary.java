package com.example.employeeapp.service;

import com.example.employeeapp.domain.Department;

/**
 * 画面に出す1行分の集計。
 *
 * DBから来る生の集計（DepartmentStatusCount）を、
 * サービス層で「1部署＝1行」の形に組み替えたもの。
 */
public record DepartmentSummary(Department department,
                                long active,
                                long leave,
                                long retired) {

    /** 合計。画面で足し算させないため、ここで持つ */
    public long total() {
        return active + leave + retired;
    }

    /**
     * 全体に対する割合（％）。棒グラフの長さに使う。
     * 0除算を避けるため、母数が0なら0を返す。
     */
    public int ratio(long grandTotal) {
        if (grandTotal == 0) {
            return 0;
        }
        return (int) Math.round(total() * 100.0 / grandTotal);
    }
}
