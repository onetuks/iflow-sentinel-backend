package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.domain.rule.Severity;
import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.ChannelAuth;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 픽스처에는 ProcessDirect 채널이 없어(HTTPS/DataStoreConsumer/HTTP뿐) 이 evaluator만 최소
 * 합성 모델로 검증한다.
 * 서로 다른 두 "아티팩트"(배치를 흉내낸 models 리스트)에 걸쳐 sender/receiver 짝짓기가 이뤄지는지 확인한다.
 */
class ProcessDirectPairingEvaluatorTest {

    private final ProcessDirectPairingEvaluator evaluator = new ProcessDirectPairingEvaluator();

    @Test
    void receiverWithoutMatchingSenderAcrossArtifactsIsFlagged() {
        Channel sender = processDirectChannel("ch1", "Sender", "/pd/paired");
        Channel pairedReceiver = processDirectChannel("ch2", "Receiver", "/pd/paired");
        Channel unpairedReceiver = processDirectChannel("ch3", "Receiver", "/pd/unpaired");

        Artifact artifact1 = RuleEngineTestSupport.artifact();
        Artifact artifact2 = RuleEngineTestSupport.artifact();
        ParsedModel model1 = RuleEngineTestSupport.modelWithChannels(List.of(sender, pairedReceiver));
        ParsedModel model2 = RuleEngineTestSupport.modelWithChannels(List.of(unpairedReceiver));

        Rule rule = RuleEngineTestSupport.rule(RuleType.PROCESSDIRECT_PAIRING, Map.of(), Map.of(),
                "대응하는 sender가 없습니다.");
        EffectiveRule effectiveRule = new EffectiveRule(rule, Severity.FAIL, true);

        List<FindingResult> findings = evaluator.evaluate(effectiveRule, List.of(
                new ArtifactParsedModel(artifact1, model1),
                new ArtifactParsedModel(artifact2, model2)));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).artifact()).isEqualTo(artifact2);
        assertThat(findings.get(0).location()).isEqualTo("channel:ch3");
    }

    private static Channel processDirectChannel(String id, String direction, String address) {
        return new Channel(id, "PD", "src", "tgt", direction, "ProcessDirect", null, null, null, "system",
                address, new ChannelAuth(null, null, null, null), List.of(), Map.of());
    }
}
