package com.example.employeeapp.domain;

/**
 * 利用者の権限。
 *
 * 業務系では「見られる人」と「変えられる人」を分けるのが基本。
 * 全員が編集できると、誰が変えたか分からない状態になり、
 * 監査（いつ誰が何をしたかの説明）ができなくなる。
 *
 * Spring Security は権限を "ROLE_" で始まる文字列として扱う決まりがあるので、
 * その変換をここに閉じ込めている。呼び出す側は Role だけを見ればよい。
 */
public enum Role {

    /** 登録・編集・退職・削除ができる */
    ADMIN("管理者"),

    /** 一覧・検索・集計・CSV出力のみ。更新系の操作はできない */
    VIEWER("閲覧のみ");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Spring Security に渡す形式（ROLE_ADMIN / ROLE_VIEWER） */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
