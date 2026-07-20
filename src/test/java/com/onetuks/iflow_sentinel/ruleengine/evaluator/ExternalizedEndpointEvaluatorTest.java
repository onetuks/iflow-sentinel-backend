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

/** 실제 픽스처의 유일한 Receiver 채널(HTTP)은 {{Receiver_Address}}로 외부화되어 있어 위반이 없어야 한다. */
class ExternalizedEndpointEvaluatorTest {

    private final ExternalizedEndpointEvaluator evaluator = new ExternalizedEndpointEvaluator();
    private final ParsedModel parsedModel = RuleEngineTestSupport.realFixtureModel();
    private final Artifact artifact = RuleEngineTestSupport.artifact();

    @Test
    void receiverChannelWithExternalizedAddressProducesNoFindings() {
        Rule rule = RuleEngineTestSupport.rule(
                RuleType.EXTERNALIZED_ENDPOINT, Map.of("direction", "Receiver"), Map.of(), "수신 주소는 외부화되어야 합니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule,
                List.of(new ArtifactParsedModel(artifact, parsedModel)));

        assertThat(findings).isEmpty();
    }
}
