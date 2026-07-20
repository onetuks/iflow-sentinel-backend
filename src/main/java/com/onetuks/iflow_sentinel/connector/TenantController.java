package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.dto.TenantRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public TenantResponse create(@RequestBody TenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping
    public List<TenantResponse> list(@RequestParam(required = false) Long projectId) {
        return tenantService.list(projectId);
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PostMapping("/{id}/test-connection")
    public ConnectionTestResult testConnection(@PathVariable Long id) {
        return tenantService.testConnection(id);
    }
}
