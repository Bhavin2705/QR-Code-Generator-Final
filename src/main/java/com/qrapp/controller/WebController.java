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
}
