package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;

import java.util.List;

/** RuleType 하나에 대한 평가 로직. SINGLE 스코프 규칙도 models 전체를 받되 대개 각 모델을 독립적으로 순회한다. */
public interface RuleTypeEvaluator {

    RuleType supports();

    List<FindingResult> evaluate(EffectiveRule rule, List<ArtifactParsedModel> models);
}
