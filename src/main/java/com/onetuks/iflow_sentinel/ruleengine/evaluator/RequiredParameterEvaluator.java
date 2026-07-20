package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.parser.model.Parameter;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** params.names[]에 나열된 외부화 파라미터가 parameters[]에 선언되어 있지 않으면 위반으로 본다. */
@Component
public class RequiredParameterEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.REQUIRED_PARAMETER;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> params = effectiveRule.rule().getParams();
        List<String> requiredNames = RuleParams.stringList(params, "names");

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            Set<String> declared = model.parsedModel().parameters().stream().map(Parameter::name).collect(Collectors.toSet());
            for (String requiredName : requiredNames) {
                if (!declared.contains(requiredName)) {
                    findings.add(new FindingResult(
                            model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                            "parameter:" + requiredName, effectiveRule.rule().getMessage()
                    ));
                }
            }
        }
        return findings;
    }
}
