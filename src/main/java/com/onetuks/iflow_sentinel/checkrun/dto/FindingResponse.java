package com.onetuks.iflow_sentinel.checkrun.dto;

import com.onetuks.iflow_sentinel.domain.finding.Finding;
import com.onetuks.iflow_sentinel.domain.rule.Severity;

public record FindingResponse(Long id, Long artifactId, Long ruleId, String ruleKey, Severity severity, String location, String message) {

    public static FindingResponse from(Finding finding) {
        return new FindingResponse(
                finding.getId(),
                finding.getArtifact().getId(),
                finding.getRule().getId(),
                finding.getRule().getRuleKey(),
                finding.getSeverity(),
                finding.getLocation(),
                finding.getMessage()
        );
    }
}
