package com.example.employeeapp.web;

import com.example.employeeapp.domain.Department;
import com.example.employeeapp.domain.Employee;
import com.example.employeeapp.domain.EmploymentStatus;
import com.example.employeeapp.service.DepartmentSummary;
import com.example.employeeapp.service.DuplicateEmployeeNumberException;
import com.example.employeeapp.service.EmployeeService;
import com.example.employeeapp.service.StaleEmployeeException;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 画面担当（コントローラ）。
 *
 * 役割は「URLを受け取る → サービスを呼ぶ → 表示するデータを詰める → 画面名を返す」だけ。
 * ここに業務ルール（重複判定・区分の意味など）を書き始めると、
 * 画面が増えたときに同じ判定をコピーすることになる。
 */
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    /** すべての画面で使う選択肢を、毎回自動で渡す */
    @ModelAttribute("departments")
    public Department[] departments() {
        return Department.values();
    }

    @ModelAttribute("statuses")
    public EmploymentStatus[] statuses() {
        return EmploymentStatus.values();
    }

    /** 一覧＋検索 */
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) Department department,
                       @RequestParam(required = false) EmploymentStatus status,
                       @PageableDefault(size = 10, sort = "employeeNumber",
                               direction = Sort.Direction.ASC) Pageable pageable,
                       Model model) {

        Page<Employee> page = service.search(keyword, department, status, pageable);

        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("department", department);
        model.addAttribute("status", status);
        return "employees/list";
    }

    /**
     * 部署ごとの人数（集計画面）。
     *
     * 合計はサービス層から来た値を足し合わせるだけ。
     * 画面（Thymeleaf）で計算させると、同じ計算が他の画面にも散らばる。
     */
    @GetMapping("/summary")
    public String summary(Model model) {
        List<DepartmentSummary> summaries = service.summarizeByDepartment();

        long grandTotal = summaries.stream().mapToLong(DepartmentSummary::total).sum();

        model.addAttribute("summaries", summaries);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("totalActive",
                summaries.stream().mapToLong(DepartmentSummary::active).sum());
        model.addAttribute("totalLeave",
                summaries.stream().mapToLong(DepartmentSummary::leave).sum());
        model.addAttribute("totalRetired",
                summaries.stream().mapToLong(DepartmentSummary::retired).sum());
        return "employees/summary";
    }

    /** 新規登録画面を開く */
    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("employeeForm")) {
            model.addAttribute("employeeForm", new EmployeeForm());
        }
        model.addAttribute("editing", false);
        return "employees/form";
    }

    /** 新規登録の実行 */
    @PostMapping
    public String create(@Valid @ModelAttribute("employeeForm") EmployeeForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        // 入力チェックで引っかかったら、入力内容を残したまま画面に戻す
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "employees/form";
        }

        try {
            Employee saved = service.create(form);
            redirectAttributes.addFlashAttribute("message",
                    "社員「" + saved.getName() + "」を登録しました");
        } catch (DuplicateEmployeeNumberException e) {
            // 業務ルール違反は、該当項目の下にエラーとして表示する
            bindingResult.rejectValue("employeeNumber", "duplicate", e.getMessage());
            model.addAttribute("editing", false);
            return "employees/form";
        }

        // 登録後はリダイレクトする（PRGパターン）。
        // そのまま画面を返すと、ブラウザの再読み込みで二重登録される。
        return "redirect:/employees";
    }

    /** 編集画面を開く */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("employeeForm")) {
            model.addAttribute("employeeForm", toForm(service.findById(id)));
        }
        model.addAttribute("editing", true);
        model.addAttribute("employeeId", id);
        return "employees/form";
    }

    /** 更新の実行 */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("employeeForm") EmployeeForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("employeeId", id);
            return "employees/form";
        }

        try {
            Employee saved = service.update(id, form);
            redirectAttributes.addFlashAttribute("message",
                    "社員「" + saved.getName() + "」を更新しました");
        } catch (DuplicateEmployeeNumberException e) {
            bindingResult.rejectValue("employeeNumber", "duplicate", e.getMessage());
            model.addAttribute("editing", true);
            model.addAttribute("employeeId", id);
            return "employees/form";
        } catch (StaleEmployeeException e) {
            /*
             * 編集画面を開いている間に、他の人が同じ社員を更新していた場合。
             *
             * 入力内容をそのまま残すと、利用者は自分の入力を保存できたと錯覚しやすい。
             * ここでは最新の内容を読み直して画面に出し、
             * 「今こうなっている。この上で直すかどうか決めてください」という形にする。
             * どちらを採用するかをシステムが勝手に決めない。
             */
            model.addAttribute("employeeForm", toForm(service.findById(id)));
            model.addAttribute("conflictMessage", e.getMessage());
            model.addAttribute("editing", true);
            model.addAttribute("employeeId", id);
            return "employees/form";
        }

        return "redirect:/employees";
    }

    /** 退職にする（論理削除） */
    @PostMapping("/{id}/retire")
    public String retire(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Employee employee = service.findById(id);
        service.retire(id);
        redirectAttributes.addFlashAttribute("message",
                "社員「" + employee.getName() + "」を退職にしました");
        return "redirect:/employees";
    }

    /** 完全に削除する（物理削除） */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Employee employee = service.findById(id);
        service.delete(id);
        redirectAttributes.addFlashAttribute("message",
                "社員「" + employee.getName() + "」を削除しました");
        return "redirect:/employees";
    }

    /**
     * CSV出力。
     *
     * 業務系では「画面で見た一覧をExcelで受け取りたい」という要望が必ず来る。
     * 先頭にBOMを付けているのは、付けないとExcelが日本語を文字化けさせるため。
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(@RequestParam(defaultValue = "") String keyword,
                                      @RequestParam(required = false) Department department,
                                      @RequestParam(required = false) EmploymentStatus status) {

        List<Employee> employees = service.searchAll(keyword, department, status);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        StringBuilder csv = new StringBuilder();
        csv.append("社員番号,氏名,フリガナ,部署,メールアドレス,入社日,在籍区分,備考\r\n");
        for (Employee e : employees) {
            csv.append(quote(e.getEmployeeNumber())).append(',')
               .append(quote(e.getName())).append(',')
               .append(quote(e.getNameKana())).append(',')
               .append(quote(e.getDepartment().getLabel())).append(',')
               .append(quote(e.getEmail())).append(',')
               .append(quote(e.getHireDate().format(dateFormat))).append(',')
               .append(quote(e.getStatus().getLabel())).append(',')
               .append(quote(e.getNote()))
               .append("\r\n");
        }

        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(body, 0, withBom, bom.length, body.length);

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employees.csv\"")
                .body(withBom);
    }

    /**
     * CSVの1項目をダブルクォートで囲む。
     * 値の中にダブルクォートがあれば2つ重ねるのがCSVの決まり（RFC 4180）。
     * これを忘れると、備考にカンマが入った瞬間に列がずれる。
     */
    private String quote(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** エンティティ → フォーム（編集画面に初期表示するため） */
    private EmployeeForm toForm(Employee e) {
        EmployeeForm form = new EmployeeForm();
        form.setId(e.getId());
        form.setEmployeeNumber(e.getEmployeeNumber());
        form.setName(e.getName());
        form.setNameKana(e.getNameKana());
        form.setDepartment(e.getDepartment());
        form.setEmail(e.getEmail());
        form.setHireDate(e.getHireDate());
        form.setStatus(e.getStatus());
        form.setNote(e.getNote());
        // 排他制御用。画面の hidden 項目として往復させる
        form.setVersion(e.getVersion());
        return form;
    }
}
