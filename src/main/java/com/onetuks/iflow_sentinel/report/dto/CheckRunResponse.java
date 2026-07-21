package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunStatus;
import com.onetuks.iflow_sentinel.report.domain.finding.Finding;

import java.util.List;
import java.util.Map;

public record CheckRunResponse(
        Long id,
        Long projectId,
        CheckRunStatus status,
        Map<String, Object> summary,
        List<FindingResponse> findings
) {
    public static CheckRunResponse from(CheckRun checkRun, List<Finding> findings) {
        return new CheckRunResponse(
                checkRun.getId(),
                checkRun.getProject().getId(),
                checkRun.getStatus(),
                checkRun.getSummary(),
                findings.stream().map(FindingResponse::from).toList()
        );
    }
}
