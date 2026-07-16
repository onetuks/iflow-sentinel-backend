package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.TenantCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse.TenantResponses;
import com.onetuks.iflow_sentinel.connector.dto.TenantUpdateRequest;
import com.onetuks.iflow_sentinel.connector.service.TenantService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = "/api/connectors/tenants",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TenantRestController {

  private final TenantService tenantService;

  @PostMapping
  public ResponseEntity<String> createTenant(@RequestBody @Valid TenantCreateRequest request) {
    Tenant result = tenantService.createTenant(request);
    return ResponseEntity.created(URI.create("/api/connectors/tenants/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editTenant(@PathVariable Long id, @RequestBody @Valid TenantUpdateRequest request) {
    tenantService.updateTenant(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<TenantResponse> searchTenant(@PathVariable Long id) {
    Tenant result = tenantService.getTenantById(id);
    return ResponseEntity.ok(TenantResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<TenantResponses> searchTenants(@PageableDefault Pageable pageable) {
    Page<Tenant> results = tenantService.getTenants(pageable);
    return ResponseEntity.ok(TenantResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
    tenantService.removeTenant(id);
    return ResponseEntity.noContent().build();
  }
}
