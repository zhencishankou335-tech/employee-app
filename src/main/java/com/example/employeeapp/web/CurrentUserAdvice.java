package com.example.employeeapp.web;

import com.example.employeeapp.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * すべての画面に「ログイン中の人」の表示名と権限名を渡す。
 *
 * 各コントローラで毎回 model に詰めると、追加した画面で入れ忘れて
 * ヘッダの名前だけ消える、という抜けが起きる。
 * @ControllerAdvice に置けば全画面に自動で渡る。
 *
 * 画面（Thymeleaf）から principal を直接たどることもできるが、
 * その書き方だとテストでログイン利用者を差し替えたときに壊れやすい。
 * ここで Java 側に寄せて、画面は単なる文字列を受け取るだけにしている。
 */
@ControllerAdvice
public class CurrentUserAdvice {

    /** ヘッダに出す表示名。未ログインなら null（ログイン画面ではヘッダ自体を出さない） */
    @ModelAttribute("loginUserName")
    public String loginUserName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AppUserDetails details) {
            return details.getDisplayName();
        }
        // テストなどで別の形の利用者が入ってきた場合の保険
        return authentication.getName();
    }

    /** ヘッダに出す権限名（「管理者」「閲覧のみ」） */
    @ModelAttribute("loginUserRole")
    public String loginUserRole(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AppUserDetails details) {
            return details.getRole().getLabel();
        }
        return null;
    }
}
