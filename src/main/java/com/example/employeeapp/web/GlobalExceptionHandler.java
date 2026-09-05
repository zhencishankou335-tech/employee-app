package com.example.employeeapp.web;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
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
        return "error/message";
    }
}
