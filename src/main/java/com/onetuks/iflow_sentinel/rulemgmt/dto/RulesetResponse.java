package com.onetuks.iflow_sentinel.rulemgmt.dto;

import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;

import java.util.List;

public record RulesetResponse(
        Long id,
        String rulesetKey,
        String version,
        String description,
        List<RuleResponse> rules,
        List<Long> importedRulesetIds
) {
    public static RulesetResponse from(Ruleset ruleset) {
        return new RulesetResponse(
                ruleset.getId(),
                ruleset.getRulesetKey(),
                ruleset.getVersion(),
                ruleset.getDescription(),
                ruleset.getRules().stream().map(RuleResponse::from).toList(),
                ruleset.getImports().stream().map(Ruleset::getId).toList()
        );
    }
}
