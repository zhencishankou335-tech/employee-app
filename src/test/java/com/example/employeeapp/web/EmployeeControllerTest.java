package com.example.employeeapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.service.DuplicateEmployeeNumberException;
import com.example.employeeapp.service.EmployeeService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 登録画面のテスト。
 *
 * @WebMvcTest は画面まわりだけを読み込む。DBは使わないので、
 * サービスは @MockitoBean で差し替えた偽物を使う。
 * 「入力チェックが効いているか」「登録後にリダイレクトしているか」を確認する。
 */
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService service;

    @Test
    @DisplayName("正しい入力なら登録され、一覧にリダイレクトする")
    void createWithValidInput() throws Exception {
        given(service.create(any())).willReturn(employee());

        mockMvc.perform(post("/employees")
                        .param("employeeNumber", "E0099")
                        .param("name", "松本 遥")
                        .param("nameKana", "マツモト ハルカ")
                        .param("department", "DEVELOPMENT")
                        .param("email", "matsumoto@example.com")
                        .param("hireDate", "2025-04-01")
                        .param("status", "ACTIVE")
                        .param("note", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        verify(service).create(any());
    }

    @Test
    @DisplayName("社員番号の形式が違うと登録されず、画面に戻る")
    void createWithInvalidEmployeeNumber() throws Exception {
        mockMvc.perform(post("/employees")
                        .param("employeeNumber", "abc")
                        .param("name", "松本 遥")
                        .param("nameKana", "マツモト ハルカ")
                        .param("department", "DEVELOPMENT")
                        .param("email", "matsumoto@example.com")
                        .param("hireDate", "2025-04-01")
                        .param("status", "ACTIVE")
                        .param("note", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(model().attributeHasFieldErrors("employeeForm", "employeeNumber"));

        // 入力エラーのときはサービスを呼んでいないこと（＝DBに触っていないこと）
        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("必須項目が空だと、その項目にエラーが付いて画面に戻る")
    void createWithEmptyRequiredFields() throws Exception {
        mockMvc.perform(post("/employees")
                        .param("employeeNumber", "")
                        .param("name", "")
                        .param("nameKana", "")
                        .param("email", "")
                        .param("hireDate", "")
                        .param("note", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("employeeForm",
                        "employeeNumber", "name", "nameKana", "department", "hireDate", "status"));
    }

    @Test
    @DisplayName("入社日に未来の日付は入れられない")
    void createWithFutureHireDate() throws Exception {
        mockMvc.perform(post("/employees")
                        .param("employeeNumber", "E0099")
                        .param("name", "松本 遥")
                        .param("nameKana", "マツモト ハルカ")
                        .param("department", "DEVELOPMENT")
                        .param("email", "matsumoto@example.com")
                        .param("hireDate", "2099-01-01")
                        .param("status", "ACTIVE")
                        .param("note", ""))
                .andExpect(model().attributeHasFieldErrors("employeeForm", "hireDate"));
    }

    @Test
    @DisplayName("フリガナがひらがなだとエラーになる")
    void createWithHiraganaKana() throws Exception {
        mockMvc.perform(post("/employees")
                        .param("employeeNumber", "E0099")
                        .param("name", "松本 遥")
                        .param("nameKana", "まつもと はるか")
                        .param("department", "DEVELOPMENT")
                        .param("email", "matsumoto@example.com")
                        .param("hireDate", "2025-04-01")
                        .param("status", "ACTIVE")
                        .param("note", ""))
                .andExpect(model().attributeHasFieldErrors("employeeForm", "nameKana"));
    }

    @Test
    @DisplayName("社員番号が重複していると、その項目にエラーが出て画面に戻る")
    void createWithDuplicateEmployeeNumber() throws Exception {
        given(service.create(any()))
                .willThrow(new DuplicateEmployeeNumberException("E0001"));

        mockMvc.perform(post("/employees")
                        .param("employeeNumber", "E0001")
                        .param("name", "松本 遥")
                        .param("nameKana", "マツモト ハルカ")
                        .param("department", "DEVELOPMENT")
                        .param("email", "matsumoto@example.com")
                        .param("hireDate", "2025-04-01")
                        .param("status", "ACTIVE")
                        .param("note", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(model().attributeHasFieldErrors("employeeForm", "employeeNumber"));
    }

    @Test
    @DisplayName("退職ボタンを押すと論理削除が呼ばれ、行は消さない")
    void retireCallsServiceRetire() throws Exception {
        given(service.findById(eq(1L))).willReturn(employee());

        mockMvc.perform(post("/employees/1/retire"))
                .andExpect(redirectedUrl("/employees"));

        verify(service).retire(1L);
        verify(service, never()).delete(any());
    }

    private Employee employee() {
        Employee e = new Employee();
        e.setId(1L);
        e.setEmployeeNumber("E0099");
        e.setName("松本 遥");
        e.setNameKana("マツモト ハルカ");
        e.setDepartment(Department.DEVELOPMENT);
        e.setEmail("matsumoto@example.com");
        e.setHireDate(LocalDate.of(2025, 4, 1));
        e.setStatus(EmploymentStatus.ACTIVE);
        e.setNote("");
        return e;
    }
}
