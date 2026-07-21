package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** iflow.config().log()가 params.required와 다르면 iFlow 단위 Finding 1건을 만든다. */
@Component
public class RequiredLoggingEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.REQUIRED_LOGGING;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> params = effectiveRule.rule().getParams();
        String required = RuleParams.string(params, "required");

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            String actual = model.parsedModel().iflow().config().log();
            if (required != null && !required.equals(actual)) {
                findings.add(new FindingResult(
                        model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                        "iflow", effectiveRule.rule().getMessage()
                ));
            }
        }
        return findings;
    }
}
