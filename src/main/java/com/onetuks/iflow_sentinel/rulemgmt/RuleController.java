package com.onetuks.iflow_sentinel.rulemgmt;

import com.onetuks.iflow_sentinel.rulemgmt.dto.RuleRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RuleResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping("/api/rulesets/{rulesetId}/rules")
    public RuleResponse create(@PathVariable Long rulesetId, @RequestBody RuleRequest request) {
        return ruleService.create(rulesetId, request);
    }

    @GetMapping("/api/rules/{id}")
    public RuleResponse get(@PathVariable Long id) {
        return ruleService.get(id);
    }

    @PutMapping("/api/rules/{id}")
    public RuleResponse update(@PathVariable Long id, @RequestBody RuleRequest request) {
        return ruleService.update(id, request);
    }

    @DeleteMapping("/api/rules/{id}")
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }
}
