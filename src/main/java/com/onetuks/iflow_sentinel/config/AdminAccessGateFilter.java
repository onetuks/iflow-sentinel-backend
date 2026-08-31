package com.onetuks.iflow_sentinel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * SAP CPI 운영 환경을 실제로 변경하는 파괴적 엔드포인트(배포/언디플로이/삭제 등)를 공유 관리자 키로 보호한다.
 * 완전한 유저별 인증 체계가 도입되기 전까지의 최소 임시 게이트이며, 도입되면 이 필터는 통째로 교체한다.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class AdminAccessGateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminAccessGateFilter.class);
    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<ProtectedRoute> PROTECTED_ROUTES = List.of(
            new ProtectedRoute(HttpMethod.POST, "/api/tenants/*/tracker-artifacts/*/deploy"),
            new ProtectedRoute(HttpMethod.POST, "/api/tenants/*/tracker-artifacts/*/undeploy"),
            new ProtectedRoute(HttpMethod.DELETE, "/api/tenants/*/tracker-artifacts/*"),
            new ProtectedRoute(HttpMethod.DELETE, "/api/packages/*/artifacts/*"),
            new ProtectedRoute(HttpMethod.DELETE, "/api/tenants/*"),
            new ProtectedRoute(HttpMethod.PUT, "/api/tenants/*/log-level"));

    private final String adminKey;

    public AdminAccessGateFilter(@Value("${iflow-sentinel.security.admin-key:}") String adminKey) {
        this.adminKey = adminKey;
    }

    @PostConstruct
    void warnIfAdminKeyMissing() {
        if (!StringUtils.hasText(adminKey)) {
            log.error("[ADMIN GATE] iflow-sentinel.security.admin-key(ADMIN_GATE_KEY)가 설정되지 않아 "
                    + "배포/언디플로이/삭제 등 파괴적 작업이 전면 차단됩니다.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isProtectedRoute(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(adminKey)) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "관리자 키가 서버에 설정되지 않아 요청을 처리할 수 없습니다.");
            return;
        }

        String providedKey = request.getHeader(ADMIN_KEY_HEADER);
        if (!StringUtils.hasText(providedKey) || !constantTimeEquals(adminKey, providedKey)) {
            log.warn("[ADMIN GATE] 관리자 키 검증 실패: {} {} (from {})",
                    request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "관리자 키가 올바르지 않습니다.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedRoute(HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String uri = request.getRequestURI();
        return PROTECTED_ROUTES.stream()
                .anyMatch(route -> route.method() == method && PATH_MATCHER.match(route.pattern(), uri));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private record ProtectedRoute(HttpMethod method, String pattern) {
    }
}
