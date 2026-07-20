package com.onetuks.iflow_sentinel.rulemgmt;

import com.onetuks.iflow_sentinel.rulemgmt.dto.ImportRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RulesetRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RulesetResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rulesets")
public class RulesetController {

    private final RulesetService rulesetService;

    public RulesetController(RulesetService rulesetService) {
        this.rulesetService = rulesetService;
    }

    @PostMapping
    public RulesetResponse create(@RequestBody RulesetRequest request) {
        return rulesetService.create(request);
    }

    @GetMapping
    public List<RulesetResponse> list() {
        return rulesetService.list();
    }

    @GetMapping("/{id}")
    public RulesetResponse get(@PathVariable Long id) {
        return rulesetService.get(id);
    }

    @PostMapping("/{id}/imports")
    public RulesetResponse addImport(@PathVariable Long id, @RequestBody ImportRequest request) {
        return rulesetService.addImport(id, request);
    }
}
