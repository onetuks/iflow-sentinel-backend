package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** iflow.exceptionSubprocesses()가 비어 있으면 예외 처리가 없다는 뜻이므로 iFlow 단위 Finding 1건을 만든다. */
@Component
public class RequiredErrorHandlerEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.REQUIRED_ERROR_HANDLER;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            if (model.parsedModel().iflow().exceptionSubprocesses().isEmpty()) {
                findings.add(new FindingResult(
                        model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                        "iflow", effectiveRule.rule().getMessage()
                ));
            }
        }
        return findings;
    }
}
