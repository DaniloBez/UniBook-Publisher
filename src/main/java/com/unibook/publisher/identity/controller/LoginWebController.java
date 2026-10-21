package com.unibook.publisher.identity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginWebController {
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
