package com.example.employeeapp.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.example.employeeapp.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ログイン画面のテスト（devプロファイル）。
 *
 * {@link LoginPageTest} と対にして、
 * 「開発では出る / それ以外では出ない」の両方を固定する。
 * 片方だけだと、常に出ない実装にしてしまっても気づけない。
 */
/*
 * spring.h2.console.enabled=false を明示している理由：
 * devプロファイルではH2コンソールが有効になるが、@WebMvcTest は画面まわりしか読み込まないため、
 * H2コンソール用の設定クラス（H2ConsoleProperties）が存在せず、起動に失敗する。
 * このテストで確かめたいのはログイン画面の表示内容だけなので、コンソールは対象外にする。
 */
@WebMvcTest(value = LoginController.class,
        properties = "spring.h2.console.enabled=false")
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class LoginPageDevProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("devプロファイルなら、手元で試せるよう開発用のID・パスワードを表示する")
    void devCredentialsAreShownInDev() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("meibo-admin-2026!")))
                .andExpect(content().string(containsString("meibo-viewer-2026!")));
    }
}
