package com.example.employeeapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** ルート（/）を開いたら一覧に飛ばすだけ */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/employees";
    }
}
