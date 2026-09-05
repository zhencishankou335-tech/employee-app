package com.example.employeeapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeeapp.domain.AppUser;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.Role;
import com.example.employeeapp.security.AppUserDetails;
import com.example.employeeapp.security.SecurityConfig;
import com.example.employeeapp.service.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 「誰が何をできるか」のテスト。
 *
 * 画面側でボタンを隠すのは見た目の親切でしかなく、
 * URLを直接打たれたら意味がない。実際に止まっているかをここで確かめる。
 *
 * 業務系では、権限まわりは「動くこと」より「動かないこと」の確認が重要。
 * 動かないことは手で試しても見落としやすいので、テストで固定する。
 */
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService service;

    private static RequestPostProcessor loggedInAs(Role role) {
        AppUser u = new AppUser();
        u.setUsername(role == Role.ADMIN ? "admin" : "viewer");
        u.setPassword("(照合しないのでダミー)");
        u.setDisplayName(role == Role.ADMIN ? "管理者 太郎" : "閲覧 花子");
        u.setRole(role);
        u.setEnabled(true);
        return user(new AppUserDetails(u));
    }

    /** 一覧画面を描画するのに最低限必要な戻り値を用意する */
    private void stubEmptyList() {
        Page<Employee> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(service.search(any(), any(), any(), any())).willReturn(empty);
    }

    @Test
    @DisplayName("ログインしていなければ、一覧を開こうとするとログイン画面に飛ばされる")
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("閲覧のみの利用者でも、一覧は見られる")
    void viewerCanSeeList() throws Exception {
        stubEmptyList();

        mockMvc.perform(get("/employees").with(loggedInAs(Role.VIEWER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("閲覧のみの利用者は、新規登録画面をURL直打ちでも開けない")
    void viewerCannotOpenCreateForm() throws Exception {
        mockMvc.perform(get("/employees/new").with(loggedInAs(Role.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("閲覧のみの利用者は、登録を実行できない")
    void viewerCannotCreate() throws Exception {
        mockMvc.perform(post("/employees").with(loggedInAs(Role.VIEWER)).with(csrf())
                        .param("employeeNumber", "E0099")
                        .param("name", "松本 遥")
                        .param("nameKana", "マツモト ハルカ")
                        .param("department", "DEVELOPMENT")
                        .param("hireDate", "2025-04-01")
                        .param("status", "ACTIVE"))
                .andExpect(status().isForbidden());

        // 権限で弾かれた時点で、業務処理まで到達していないこと
        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("閲覧のみの利用者は、削除を実行できない")
    void viewerCannotDelete() throws Exception {
        mockMvc.perform(post("/employees/1/delete").with(loggedInAs(Role.VIEWER)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(service, never()).delete(any());
    }

    @Test
    @DisplayName("管理者は新規登録画面を開ける")
    void adminCanOpenCreateForm() throws Exception {
        mockMvc.perform(get("/employees/new").with(loggedInAs(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRFトークンが無いPOSTは、管理者であっても拒否される")
    void postWithoutCsrfTokenIsRejected() throws Exception {
        // 他サイトに置かれた偽フォームから送られてきた場合を想定している。
        // ログイン済みのブラウザでも、トークンが無ければ実行されない。
        mockMvc.perform(post("/employees").with(loggedInAs(Role.ADMIN))
                        .param("employeeNumber", "E0099")
                        .param("name", "松本 遥")
                        .param("nameKana", "マツモト ハルカ")
                        .param("department", "DEVELOPMENT")
                        .param("hireDate", "2025-04-01")
                        .param("status", "ACTIVE"))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any());
    }
}
