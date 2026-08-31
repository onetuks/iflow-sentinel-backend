package com.onetuks.iflow_sentinel.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccessGateFilterTest {

    private static final String ADMIN_KEY = "test-admin-key";

    @Test
    void blocksProtectedRouteWhenAdminKeyHeaderMissing() throws Exception {
        AdminAccessGateFilter filter = new AdminAccessGateFilter(ADMIN_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenants/1/tracker-artifacts/abc/deploy");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void blocksProtectedRouteWhenAdminKeyIsWrong() throws Exception {
        AdminAccessGateFilter filter = new AdminAccessGateFilter(ADMIN_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/tenants/1/tracker-artifacts/abc");
        request.addHeader("X-Admin-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void allowsProtectedRouteWhenAdminKeyMatches() throws Exception {
        AdminAccessGateFilter filter = new AdminAccessGateFilter(ADMIN_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/tenants/1");
        request.addHeader("X-Admin-Key", ADMIN_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void allowsUnprotectedRouteRegardlessOfHeader() throws Exception {
        AdminAccessGateFilter filter = new AdminAccessGateFilter(ADMIN_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants/1/tracker-artifacts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksProtectedRouteWhenAdminKeyNotConfigured() throws Exception {
        AdminAccessGateFilter filter = new AdminAccessGateFilter("");

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/tenants/1");
        request.addHeader("X-Admin-Key", "anything");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(chain.getRequest()).isNull();
    }
}
