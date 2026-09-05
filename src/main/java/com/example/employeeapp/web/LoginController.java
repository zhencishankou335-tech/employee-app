package com.example.employeeapp.web;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ログイン画面と、権限不足のときの画面。
 *
 * ログインの処理そのもの（IDとパスワードの照合、セッションの作成）は
 * Spring Security がやるので、ここには書かない。
 * このクラスがやるのは「画面を出す」だけ。
 */
@Controller
public class LoginController {

    private final Environment environment;

    public LoginController(Environment environment) {
        this.environment = environment;
    }

    /**
     * ログイン画面。
     *
     * URLに ?error / ?logout が付いてくる（SecurityConfig で指定している）。
     * 画面側でその有無を見てメッセージを出し分ける。
     */
    @GetMapping("/login")
    public String login(Model model) {
        /*
         * 練習用アプリなので、ログイン画面に確認用のID/パスワードを表示している。
         * ただし「本番前に消す」という手作業に頼ると、いつか必ず消し忘れる。
         * dev プロファイルのときだけ true になるようにして、
         * 本番プロファイルでは何もしなくても表示されないようにする。
         *
         * 実測で見つけた問題への対処：
         * 当初はHTMLに直接書いていたため、prodプロファイルで起動しても
         * ログイン画面に admin / admin12345 が表示されていた。
         */
        model.addAttribute("showDevCredentials",
                environment.acceptsProfiles(Profiles.of("dev")));
        return "login";
    }

    /**
     * 権限が足りないとき。
     *
     * 例：閲覧のみの利用者が /employees/new を直接URLで開いた場合。
     * 何も設定しないと真っ白な403ページが返り、利用者には何が起きたか分からない。
     *
     * @GetMapping ではなく @RequestMapping にしてある理由：
     * Spring Security はここへ「フォワード」で飛ばすので、
     * 元のリクエストがPOSTなら、この画面にもPOSTで届く。
     * GET専用にしていると、権限エラーのはずが 405（許可されていないメソッド）になり、
     * 利用者にも開発者にも何が起きたか分からなくなる。
     * 実測で発見した（管理者以外が削除を実行したとき 403 ではなく 405 が返っていた）。
     */
    @RequestMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("errorTitle", "この操作を行う権限がありません");
        model.addAttribute("errorDetail",
                "登録・編集・削除は管理者権限が必要です。"
                + "権限が必要な場合はシステム管理者に連絡してください。");
        return "error/message";
    }
}
