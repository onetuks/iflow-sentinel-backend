package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageResponse;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageResponse.IntegrationPackageResponses;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageUpdateRequest;
import com.onetuks.iflow_sentinel.connector.service.IntegrationPackageService;
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
    path = "/api/connectors/integration-packages",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class IntegrationPackageRestController {

  private final IntegrationPackageService integrationPackageService;

  @PostMapping
  public ResponseEntity<String> createIntegrationPackage(@RequestBody @Valid IntegrationPackageCreateRequest request) {
    IntegrationPackage result = integrationPackageService.createIntegrationPackage(request);
    return ResponseEntity.created(URI.create("/api/connectors/integration-packages/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editIntegrationPackage(@PathVariable Long id, @RequestBody @Valid IntegrationPackageUpdateRequest request) {
    integrationPackageService.updateIntegrationPackage(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<IntegrationPackageResponse> searchIntegrationPackage(@PathVariable Long id) {
    IntegrationPackage result = integrationPackageService.getIntegrationPackageById(id);
    return ResponseEntity.ok(IntegrationPackageResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<IntegrationPackageResponses> searchIntegrationPackages(@PageableDefault Pageable pageable) {
    Page<IntegrationPackage> results = integrationPackageService.getIntegrationPackages(pageable);
    return ResponseEntity.ok(IntegrationPackageResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteIntegrationPackage(@PathVariable Long id) {
    integrationPackageService.removeIntegrationPackage(id);
    return ResponseEntity.noContent().build();
  }
}
