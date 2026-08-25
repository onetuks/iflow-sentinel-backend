package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.report.domain.finding.Finding;
import com.onetuks.iflow_sentinel.rule.domain.Severity;

public record FindingResponse(
        Long id,
        String artifactId,
        Long ruleId,
        String ruleKey,
        Severity severity,
        String location,
        String message) {

    public static FindingResponse from(Finding finding) {
        return new FindingResponse(
                finding.getId(),
                finding.getArtifact() != null ? finding.getArtifact().getSapArtifactId() : null,
                finding.getRule().getId(),
                finding.getRule().getRuleKey(),
                finding.getSeverity(),
                finding.getLocation(),
                finding.getMessage());
    }
}
