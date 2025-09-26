package com.qrapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class WebController {
    @GetMapping("")
    public String redirectToStaticIndex() {
        return "redirect:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    @GetMapping("/signup")
    public String signup() {
        return "redirect:/signup.html";
    }

    @GetMapping("/admin")
    public String adminPage() {
        // Always serve the static admin page; the client-side script will
        // check the user's token/role and redirect to admin-login.html if needed.
        return "redirect:/admin.html";
    }
}
