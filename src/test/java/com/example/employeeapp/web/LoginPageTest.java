package com.example.employeeapp.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeeapp.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ログイン画面のテスト（devプロファイルではない状態）。
 *
 * 確認したいのは見た目ではなく、
 * **開発用のID/パスワードが本番の画面に出ないこと**。
 *
 * この種の「本番前に消す」ものは、手作業の運用にすると必ず消し忘れる。
 * 実際、最初の実装ではHTMLに直接書いていたため、
 * prodプロファイルで起動しても画面に表示されていた（実測で発見）。
 * 同じ間違いを二度としないよう、テストで固定する。
 */
@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
class LoginPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ログイン画面はログインしていなくても開ける")
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @Test
    @DisplayName("ログイン画面にはCSRFトークンが埋め込まれる")
    void loginPageHasCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @DisplayName("devプロファイルでなければ、開発用のID・パスワードを画面に出さない")
    void devCredentialsAreHiddenByDefault() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(not(containsString("admin12345"))))
                .andExpect(content().string(not(containsString("viewer12345"))));
    }
}
