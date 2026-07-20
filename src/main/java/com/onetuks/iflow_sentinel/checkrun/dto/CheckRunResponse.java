package com.onetuks.iflow_sentinel.checkrun.dto;

import com.onetuks.iflow_sentinel.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRunStatus;
import com.onetuks.iflow_sentinel.domain.finding.Finding;

import java.util.List;
import java.util.Map;

public record CheckRunResponse(
        Long id,
        Long projectId,
        Long rulesetId,
        CheckRunStatus status,
        Map<String, Object> summary,
        List<FindingResponse> findings
) {
    public static CheckRunResponse from(CheckRun checkRun, List<Finding> findings) {
        return new CheckRunResponse(
                checkRun.getId(),
                checkRun.getProject().getId(),
                checkRun.getRuleset().getId(),
                checkRun.getStatus(),
                checkRun.getSummary(),
                findings.stream().map(FindingResponse::from).toList()
        );
    }
}
