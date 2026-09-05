package com.example.employeeapp.domain;

/**
 * 部署。
 *
 * 業務系では「決まった選択肢しか入らない項目」が非常に多い。
 * 文字列で持つと表記ゆれ（"営業部" / "営業" / "営業課"）が必ず発生するので、
 * enum で選択肢を固定する。
 */
public enum Department {

    SALES("営業部"),
    DEVELOPMENT("開発部"),
    ACCOUNTING("経理部"),
    HR("人事部"),
    GENERAL_AFFAIRS("総務部");

    /** 画面に表示する日本語名 */
    private final String label;

    Department(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
