package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
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

/** 실제 픽스처에는 operation=put인 DBstorage 스텝이 3개, operation=delete인 스텝이 1개 있다. */
class ForbiddenConfigurationEvaluatorTest {

    private final ForbiddenConfigurationEvaluator evaluator = new ForbiddenConfigurationEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void matchingKeyValueFlagsEveryMatchingStep() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.FORBIDDEN_CONFIGURATION, Map.of("element", "step"), Map.of("key", "operation", "value", "put"), "put 연산은 금지됩니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.WARN, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(3);
    }

    @Test
    void nonMatchingKeyProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.FORBIDDEN_CONFIGURATION, Map.of("element", "step"), Map.of("key", "nonexistentKey", "value", "x"), "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.WARN, true);

        assertThat(evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel)))).isEmpty();
    }
}
