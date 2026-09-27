package com.dohun.nfcreview.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * (4부) 로그인 화면만 보여줘요.
 * 폼 제출(POST /admin/login)은 스프링 시큐리티가 컨트롤러보다 먼저 가로채서 처리해요.
 */
@Controller
public class AdminLoginController {

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }
}
