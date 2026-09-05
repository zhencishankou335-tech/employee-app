package com.example.employeeapp.service;

/**
 * 編集画面を開いている間に、他の人が同じ社員を更新していたときに投げる例外。
 *
 * このまま保存させると、先に保存した人の変更が黙って消える（ロストアップデート）。
 * 業務系では「保存できませんでした」と止めて、最新の状態を見せ直すのが定石。
 * 勝手にどちらかを採用したり、項目ごとに混ぜたりしてはいけない。
 */
public class StaleEmployeeException extends RuntimeException {

    public StaleEmployeeException(String name) {
        super("社員「" + name + "」は、あなたが編集画面を開いたあとに"
              + "他の利用者によって更新されています。"
              + "最新の内容を読み込みましたので、確認してから保存し直してください。");
    }
}
