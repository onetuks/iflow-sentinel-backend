package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.rule.domain.RuleType;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** target.direction으로 필터된 채널 중 externalizedRefs가 비어 있으면(주소 하드코딩) 위반으로 본다. */
@Component
public class ExternalizedEndpointEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.EXTERNALIZED_ENDPOINT;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> target = effectiveRule.rule().getTarget();
        String direction = RuleParams.string(target, "direction");

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            for (Channel channel : model.parsedModel().iflow().channels()) {
                if (direction != null && !direction.equals(channel.direction())) {
                    continue;
                }
                if (channel.externalizedRefs().isEmpty()) {
                    findings.add(new FindingResult(
                            model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                            "channel:" + channel.id(), effectiveRule.rule().getMessage()));
                }
            }
        }
        return findings;
    }
}
