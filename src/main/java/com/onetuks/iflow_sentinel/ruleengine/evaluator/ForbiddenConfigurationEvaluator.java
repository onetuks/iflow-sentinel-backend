package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** target.element(channel/step)의 properties{}에 params.key==params.value인 설정이 있으면 위반으로 본다. */
@Component
public class ForbiddenConfigurationEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.FORBIDDEN_CONFIGURATION;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> target = effectiveRule.rule().getTarget();
        Map<String, Object> params = effectiveRule.rule().getParams();
        String element = RuleParams.string(target, "element");
        String forbiddenKey = RuleParams.string(params, "key");
        String forbiddenValue = RuleParams.string(params, "value");

        List<FindingResult> findings = new ArrayList<>();
        if (forbiddenKey == null) {
            return findings;
        }
        for (ArtifactParsedModel model : models) {
            if ("channel".equals(element)) {
                model.parsedModel().iflow().channels().forEach(channel -> {
                    if (hasForbiddenValue(channel.properties(), forbiddenKey, forbiddenValue)) {
                        findings.add(new FindingResult(model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                                "channel:" + channel.id(), effectiveRule.rule().getMessage()));
                    }
                });
            } else if ("step".equals(element)) {
                model.parsedModel().iflow().steps().forEach(step -> {
                    if (hasForbiddenValue(step.properties(), forbiddenKey, forbiddenValue)) {
                        findings.add(new FindingResult(model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                                "step:" + step.id(), effectiveRule.rule().getMessage()));
                    }
                });
            }
        }
        return findings;
    }

    private static boolean hasForbiddenValue(Map<String, String> properties, String key, String forbiddenValue) {
        String actual = properties.get(key);
        if (actual == null) {
            return false;
        }
        return forbiddenValue == null || forbiddenValue.equals(actual);
    }
}
