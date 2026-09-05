package com.example.employeeapp.service;

/**
 * 社員番号が既に使われているときに投げる例外。
 * 業務ルール違反はサービス層で判定し、画面側でメッセージに変換する。
 */
public class DuplicateEmployeeNumberException extends RuntimeException {

    public DuplicateEmployeeNumberException(String employeeNumber) {
        super("社員番号 " + employeeNumber + " は既に登録されています");
    }
}
