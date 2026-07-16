package com.onetuks.iflow_sentinel.report.controller;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.report.dto.CheckRunCreateRequest;
import com.onetuks.iflow_sentinel.report.dto.CheckRunResponse;
import com.onetuks.iflow_sentinel.report.dto.CheckRunResponse.CheckRunResponses;
import com.onetuks.iflow_sentinel.report.dto.CheckRunUpdateRequest;
import com.onetuks.iflow_sentinel.report.service.CheckRunService;
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
    path = "/api/reports/check-runs",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CheckRunRestController {

  private final CheckRunService checkRunService;

  @PostMapping
  public ResponseEntity<String> createCheckRun(@RequestBody @Valid CheckRunCreateRequest request) {
    CheckRun result = checkRunService.createCheckRun(request);
    return ResponseEntity.created(URI.create("/api/reports/check-runs/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editCheckRun(@PathVariable Long id, @RequestBody @Valid CheckRunUpdateRequest request) {
    checkRunService.updateCheckRun(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<CheckRunResponse> searchCheckRun(@PathVariable Long id) {
    CheckRun result = checkRunService.getCheckRunById(id);
    return ResponseEntity.ok(CheckRunResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<CheckRunResponses> searchCheckRuns(@PageableDefault Pageable pageable) {
    Page<CheckRun> results = checkRunService.getCheckRuns(pageable);
    return ResponseEntity.ok(CheckRunResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteCheckRun(@PathVariable Long id) {
    checkRunService.removeCheckRun(id);
    return ResponseEntity.noContent().build();
  }
}
