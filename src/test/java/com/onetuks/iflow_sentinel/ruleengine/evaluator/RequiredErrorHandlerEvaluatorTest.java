package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.rule.domain.Rule;
import com.onetuks.iflow_sentinel.rule.domain.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.Severity;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 픽스처는 exceptionSubprocesses가 0개다(설계서 5.3.7 실증 사례) — 반드시 위반으로 검출되어야 한다. */
class RequiredErrorHandlerEvaluatorTest {

    private final RequiredErrorHandlerEvaluator evaluator = new RequiredErrorHandlerEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void emptyExceptionSubprocessesProducesOneFinding() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.REQUIRED_ERROR_HANDLER, Map.of(), Map.of(),
                "예외 처리 서브프로세스가 필요합니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).location()).isEqualTo("iflow");
    }
}
