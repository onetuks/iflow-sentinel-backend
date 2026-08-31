package com.onetuks.iflow_sentinel.auth.controller;

import com.onetuks.iflow_sentinel.auth.dto.LoginRequest;
import com.onetuks.iflow_sentinel.auth.dto.LoginResponse;
import com.onetuks.iflow_sentinel.auth.dto.UserResponse;
import com.onetuks.iflow_sentinel.auth.service.AuthService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
