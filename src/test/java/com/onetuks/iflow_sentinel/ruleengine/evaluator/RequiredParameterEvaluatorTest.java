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

/** 실제 픽스처는 SAP_Sender 파라미터를 선언하고 있다(22개 중 하나). */
class RequiredParameterEvaluatorTest {

    private final RequiredParameterEvaluator evaluator = new RequiredParameterEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void declaredParameterProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.REQUIRED_PARAMETER, Map.of(),
                Map.of("names", List.of("SAP_Sender")), "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        assertThat(evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel))))
                .isEmpty();
    }

    @Test
    void undeclaredParameterIsFlagged() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.REQUIRED_PARAMETER, Map.of(),
                Map.of("names", List.of("SAP_Sender", "NONEXISTENT_PARAM")), "필수 파라미터가 누락되었습니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).location()).isEqualTo("parameter:NONEXISTENT_PARAM");
    }
}
