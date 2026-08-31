package com.onetuks.iflow_sentinel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// Spring Security 필터체인(order -100대)보다도 먼저 실행되어야 인증/인가 실패(401/403) 응답도 로그에 남는다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 정적 리소스(CSS, JS 등) 요청은 로깅에서 제외하고 싶다면 아래 주석을 해제하세요
        // if (request.getRequestURI().startsWith("/assets")) {
        // filterChain.doFilter(request, response);
        // return;
        // }

        long startTime = System.currentTimeMillis();
        String uri = request.getQueryString() != null
                ? request.getRequestURI() + "?" + request.getQueryString()
                : request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[API LOG] {} {} - Status: {} ({}ms)",
                    request.getMethod(),
                    uri,
                    response.getStatus(),
                    duration);
        }
    }
}
