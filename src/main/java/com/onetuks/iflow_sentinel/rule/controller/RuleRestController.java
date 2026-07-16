package com.onetuks.iflow_sentinel.rule.controller;

import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.dto.RuleCreateRequest;
import com.onetuks.iflow_sentinel.rule.dto.RuleResponse;
import com.onetuks.iflow_sentinel.rule.dto.RuleResponse.RuleResponses;
import com.onetuks.iflow_sentinel.rule.dto.RuleUpdateRequest;
import com.onetuks.iflow_sentinel.rule.service.RuleService;
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
    path = "/api/rules/rules",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RuleRestController {

  private final RuleService ruleService;

  @PostMapping
  public ResponseEntity<String> createRule(@RequestBody @Valid RuleCreateRequest request) {
    Rule result = ruleService.createRule(request);
    return ResponseEntity.created(URI.create("/api/rules/rules/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editRule(@PathVariable Long id, @RequestBody @Valid RuleUpdateRequest request) {
    ruleService.updateRule(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<RuleResponse> searchRule(@PathVariable Long id) {
    Rule result = ruleService.getRuleById(id);
    return ResponseEntity.ok(RuleResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<RuleResponses> searchRules(@PageableDefault Pageable pageable) {
    Page<Rule> results = ruleService.getRules(pageable);
    return ResponseEntity.ok(RuleResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
    ruleService.removeRule(id);
    return ResponseEntity.noContent().build();
  }
}
