package com.onetuks.iflow_sentinel.auth.service;

import com.onetuks.iflow_sentinel.auth.domain.user.User;
import com.onetuks.iflow_sentinel.auth.domain.user.UserRepository;
import com.onetuks.iflow_sentinel.auth.dto.LoginRequest;
import com.onetuks.iflow_sentinel.auth.dto.LoginResponse;
import com.onetuks.iflow_sentinel.auth.dto.UserResponse;
import com.onetuks.iflow_sentinel.auth.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + request.username()));

        String accessToken = jwtService.issueToken(user.getUsername(), user.getRole());
        return new LoginResponse(
                accessToken, "Bearer", jwtService.expirationSeconds(), user.getUsername(), user.getRole().name());
    }

    public UserResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + username));
        return new UserResponse(user.getUsername(), user.getRole().name());
    }
}
