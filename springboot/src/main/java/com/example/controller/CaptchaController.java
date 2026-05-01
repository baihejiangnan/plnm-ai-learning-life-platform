package com.example.controller;

import com.example.common.Result;
import com.example.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {
    @Autowired
    private CaptchaService captchaService;

    @GetMapping
    public Result generate() {
        return Result.success(captchaService.generate());
    }
}
