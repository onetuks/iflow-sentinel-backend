package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 픽스처의 채널 adapterType은 HTTPS(2개)/DataStoreConsumer(1개)/HTTP(1개)다. */
class AllowedAdapterTypesEvaluatorTest {

    private final AllowedAdapterTypesEvaluator evaluator = new AllowedAdapterTypesEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void nonHttpsChannelsAreFlaggedWhenOnlyHttpsAllowed() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.ALLOWED_ADAPTER_TYPES, Map.of(), Map.of("allowed", List.of("HTTPS")), "허용되지 않은 어댑터입니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(2); // DataStoreConsumer 1개 + HTTP 1개
    }

    @Test
    void allActualAdapterTypesAllowedProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.ALLOWED_ADAPTER_TYPES, Map.of(),
                Map.of("allowed", List.of("HTTPS", "DataStoreConsumer", "HTTP")), "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).isEmpty();
    }
}
