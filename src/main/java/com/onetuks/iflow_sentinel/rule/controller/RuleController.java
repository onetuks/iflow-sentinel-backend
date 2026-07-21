package com.onetuks.iflow_sentinel.rule.controller;

import com.onetuks.iflow_sentinel.rule.dto.RuleCreateRequest;
import com.onetuks.iflow_sentinel.rule.dto.RuleResponse;
import com.onetuks.iflow_sentinel.rule.dto.RuleUpdateRequest;
import com.onetuks.iflow_sentinel.rule.service.RuleService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public RuleResponse create(@RequestBody RuleCreateRequest request) {
        return ruleService.create(request);
    }

    @GetMapping("/{id}")
    public RuleResponse get(@PathVariable Long id) {
        return ruleService.get(id);
    }

    @PutMapping("/{id}")
    public RuleResponse update(@PathVariable Long id, @RequestBody RuleUpdateRequest request) {
        return ruleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }
}
