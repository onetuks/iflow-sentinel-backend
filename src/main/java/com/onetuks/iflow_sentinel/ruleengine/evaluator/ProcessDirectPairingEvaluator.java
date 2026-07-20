package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ProcessDirect 어댑터 채널을 전체 아티팩트(models) 기준으로 짝짓는다: receiver마다 대응하는 sender 주소가
 * 어딘가에 있어야 한다(설계서 5.5, CROSS 스코프). 이번 Run은 단일 아티팩트만 처리하므로 models 크기가
 * 보통 1이 되어, 아티팩트 간 짝 검증은 배치 실행(향후 CHK-002)이 갖춰지기 전까지 같은 아티팩트 내부의
 * 짝만 발견할 수 있다는 제약이 있다.
 */
@Component
public class ProcessDirectPairingEvaluator implements RuleTypeEvaluator {

    private static final String PROCESS_DIRECT = "ProcessDirect";

    @Override
    public RuleType supports() {
        return RuleType.PROCESSDIRECT_PAIRING;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Set<String> senderAddresses = new HashSet<>();
        for (ArtifactParsedModel model : models) {
            for (Channel channel : model.parsedModel().iflow().channels()) {
                if (PROCESS_DIRECT.equals(channel.adapterType()) && "Sender".equals(channel.direction())) {
                    senderAddresses.add(channel.address());
                }
            }
        }

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            for (Channel channel : model.parsedModel().iflow().channels()) {
                if (PROCESS_DIRECT.equals(channel.adapterType()) && "Receiver".equals(channel.direction())
                        && !senderAddresses.contains(channel.address())) {
                    findings.add(new FindingResult(
                            model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                            "channel:" + channel.id(), effectiveRule.rule().getMessage()
                    ));
                }
            }
        }
        return findings;
    }
}
