package com.onetuks.iflow_sentinel.report.controller;

import com.onetuks.iflow_sentinel.report.domain.finding.Finding;
import com.onetuks.iflow_sentinel.report.dto.FindingCreateRequest;
import com.onetuks.iflow_sentinel.report.dto.FindingResponse;
import com.onetuks.iflow_sentinel.report.dto.FindingResponse.FindingResponses;
import com.onetuks.iflow_sentinel.report.dto.FindingUpdateRequest;
import com.onetuks.iflow_sentinel.report.service.FindingService;
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
    path = "/api/reports/findings",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class FindingRestController {

  private final FindingService findingService;

  @PostMapping
  public ResponseEntity<String> createFinding(@RequestBody @Valid FindingCreateRequest request) {
    Finding result = findingService.createFinding(request);
    return ResponseEntity.created(URI.create("/api/reports/findings/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editFinding(@PathVariable Long id, @RequestBody @Valid FindingUpdateRequest request) {
    findingService.updateFinding(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<FindingResponse> searchFinding(@PathVariable Long id) {
    Finding result = findingService.getFindingById(id);
    return ResponseEntity.ok(FindingResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<FindingResponses> searchFindings(@PageableDefault Pageable pageable) {
    Page<Finding> results = findingService.getFindings(pageable);
    return ResponseEntity.ok(FindingResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteFinding(@PathVariable Long id) {
    findingService.removeFinding(id);
    return ResponseEntity.noContent().build();
  }
}
