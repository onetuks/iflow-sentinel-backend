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

/** 실제 픽스처의 유일한 Mapping 스텝("MM_Request")은 mapping.type()이 MessageMapping이다. */
class MappingTypeEvaluatorTest {

    private final MappingTypeEvaluator evaluator = new MappingTypeEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void messageMappingAllowedProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.MAPPING_TYPE, Map.of(), Map.of("allowed", List.of("MessageMapping")), "무시");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        assertThat(evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel)))).isEmpty();
    }

    @Test
    void disallowedMappingTypeFlagsStep() {
        Rule rule = RuleEngineTestSupport.rule(RuleType.MAPPING_TYPE, Map.of(), Map.of("allowed", List.of("OperationMapping")), "허용되지 않은 매핑 타입입니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule, List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).hasSize(1);
    }
}
