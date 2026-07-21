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

/**
 * 실제 픽스처의 sender 참여자(Sender, Smartshift, Sender1)는 모두 OP_/B2B_/CP_ 접두어를 따르지
 * 않는다.
 */
class NamingConventionEvaluatorTest {

    private final NamingConventionEvaluator evaluator = new NamingConventionEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void sendersViolatingPrefixConventionAreAllFlagged() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.NAMING_CONVENTION,
                Map.of("element", "participant", "role", "sender"),
                Map.of("prefix", List.of("OP_", "B2B_", "CP_")),
                "송신 시스템 이름은 접두어를 따라야 합니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(3);
        assertThat(findings).allMatch(f -> f.artifact() == artifact && f.severity() == Severity.FAIL);
    }

    @Test
    void matchingPrefixProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.NAMING_CONVENTION,
                Map.of("element", "participant", "role", "sender"),
                Map.of("prefix", List.of("Sender", "Smartshift")),
                "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).isEmpty();
    }
}
