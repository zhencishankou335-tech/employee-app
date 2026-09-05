package com.example.employeeapp.domain;

/**
 * 在籍区分。
 *
 * 業務系では行を物理削除せず、この種の「区分」で状態を持つことが多い。
 * 過去の伝票や履歴から参照されるため、消すと整合性が壊れるため。
 */
public enum EmploymentStatus {

    ACTIVE("在籍"),
    LEAVE("休職"),
    RETIRED("退職");

    private final String label;

    EmploymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
