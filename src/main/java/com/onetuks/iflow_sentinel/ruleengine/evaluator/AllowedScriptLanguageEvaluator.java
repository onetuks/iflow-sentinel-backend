package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** type=Script인 step 중 script.language()가 params.allowed[]에 없으면 위반으로 본다. */
@Component
public class AllowedScriptLanguageEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.ALLOWED_SCRIPT_LANGUAGE;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> params = effectiveRule.rule().getParams();
        List<String> allowed = RuleParams.stringList(params, "allowed");

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            for (StepNode step : model.parsedModel().iflow().steps()) {
                if (!"Script".equals(step.type()) || step.script() == null) {
                    continue;
                }
                if (!allowed.isEmpty() && !allowed.contains(step.script().language())) {
                    findings.add(new FindingResult(
                            model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                            "step:" + step.id(), effectiveRule.rule().getMessage()
                    ));
                }
            }
        }
        return findings;
    }
}
