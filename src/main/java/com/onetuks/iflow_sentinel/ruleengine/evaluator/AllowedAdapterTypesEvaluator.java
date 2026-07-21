package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** channel.adapterType이 params.allowed[]에 없으면 위반으로 본다. */
@Component
public class AllowedAdapterTypesEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.ALLOWED_ADAPTER_TYPES;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> params = effectiveRule.rule().getParams();
        List<String> allowed = RuleParams.stringList(params, "allowed");

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            for (Channel channel : model.parsedModel().iflow().channels()) {
                if (!allowed.isEmpty() && !allowed.contains(channel.adapterType())) {
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
