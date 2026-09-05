package com.example.employeeapp.web;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 例外をまとめて受け止める場所。
 *
 * 例えば存在しないIDのURL（/employees/9999/edit）を直接開かれたとき、
 * 何もしないと真っ白なエラー画面と500が返る。
 * 業務系では「利用者が何をすればいいか分かる画面」を返すのが最低条件なので、
 * ここで日本語のメッセージ付きの画面に差し替える。
 *
 * @ControllerAdvice を付けると、すべてのコントローラに適用される。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** 対象のデータが無い場合は 404 を返す（500ではない。サーバーは壊れていないため） */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException e, Model model) {
        model.addAttribute("errorTitle", "データが見つかりません");
        model.addAttribute("errorDetail", e.getMessage());
        model.addAttribute("errorHint",
                "URLが間違っているか、他の担当者が既に削除した可能性があります。");
        return "error/message";
    }

    /**
     * 排他制御（楽観ロック）でDB側が更新を拒否したとき。
     *
     * 編集画面からの更新は EmployeeController が先に版数を比べて止めるので、
     * 通常はここまで来ない。ここに来るのは、
     * 一覧画面から「退職」や「削除」を押したのとほぼ同時に、
     * 他の人が同じ社員を更新していた場合。
     *
     * 409 Conflict を返す。500（サーバーの故障）ではなく、
     * 「今の状態と食い違っているので処理できなかった」という意味の番号。
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(ObjectOptimisticLockingFailureException e, Model model) {
        model.addAttribute("errorTitle", "他の利用者が先に更新しました");
        model.addAttribute("errorDetail",
                "同じデータを別の利用者が更新したため、この操作は取り消されました。"
                + "一覧に戻って最新の状態を確認してから、もう一度操作してください。");
        return "error/message";
    }
}
