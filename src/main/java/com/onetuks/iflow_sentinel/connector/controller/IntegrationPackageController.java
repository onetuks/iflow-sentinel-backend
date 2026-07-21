package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageResponse;
import com.onetuks.iflow_sentinel.connector.service.IntegrationPackageService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/packages")
public class IntegrationPackageController {

    private final IntegrationPackageService integrationPackageService;

    public IntegrationPackageController(IntegrationPackageService integrationPackageService) {
        this.integrationPackageService = integrationPackageService;
    }

    @PostMapping("/sync")
    public List<IntegrationPackageResponse> sync(@PathVariable Long tenantId) {
        return integrationPackageService.sync(tenantId);
    }

    @GetMapping
    public List<IntegrationPackageResponse> list(@PathVariable Long tenantId) {
        return integrationPackageService.list(tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long tenantId, @PathVariable Long id) {
        integrationPackageService.delete(id);
    }
}
