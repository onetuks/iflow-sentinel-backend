package com.onetuks.iflow_sentinel.report.controller;

import com.onetuks.iflow_sentinel.report.dto.FindingResponse;
import com.onetuks.iflow_sentinel.report.service.FindingService;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/findings")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @GetMapping
    public List<FindingResponse> search(
            @RequestParam(required = false) Long checkRunId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) Long artifactId) {
        return findingService.search(checkRunId, severity, ruleId, artifactId);
    }
}
