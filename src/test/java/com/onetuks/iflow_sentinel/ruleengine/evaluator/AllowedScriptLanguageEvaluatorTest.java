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

/** 실제 픽스처의 유일한 Script 스텝("Check payloadMode")은 Groovy 언어를 사용한다. */
class AllowedScriptLanguageEvaluatorTest {

    private final AllowedScriptLanguageEvaluator evaluator = new AllowedScriptLanguageEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void groovyAllowedProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.ALLOWED_SCRIPT_LANGUAGE, Map.of(), Map.of("allowed", List.of("Groovy")), "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        assertThat(evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel)))).isEmpty();
    }

    @Test
    void onlyJavaScriptAllowedFlagsGroovyStep() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.ALLOWED_SCRIPT_LANGUAGE, Map.of(), Map.of("allowed", List.of("JavaScript")), "허용되지 않은 스크립트 언어입니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(1);
    }
}
