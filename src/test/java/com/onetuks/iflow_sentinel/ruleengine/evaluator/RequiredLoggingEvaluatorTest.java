package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.domain.rule.Severity;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 픽스처의 iflow.config().log()는 "All events"다. */
class RequiredLoggingEvaluatorTest {

    private final RequiredLoggingEvaluator evaluator = new RequiredLoggingEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void matchingRequiredLevelProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.REQUIRED_LOGGING, Map.of(), Map.of("required", "All events"),
                "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        assertThat(evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel))))
                .isEmpty();
    }

    @Test
    void mismatchingRequiredLevelProducesOneFinding() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.REQUIRED_LOGGING, Map.of(), Map.of("required", "Trace"),
                "로그 수준이 부족합니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.WARN, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).severity()).isEqualTo(Severity.WARN);
    }
}
