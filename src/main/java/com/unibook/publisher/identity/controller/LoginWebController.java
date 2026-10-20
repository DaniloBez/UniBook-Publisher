package com.unibook.publisher.identity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginWebController {
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/manuscripts")
    public String m1() {
        return "manuscripts";
    }

    @GetMapping("/manuscripts/my")
    public String m2() {
        return "manuscripts/my";
    }

    @GetMapping("/manuscripts/new")
    public String m3() {
        return "manuscripts/new";
    }
}
