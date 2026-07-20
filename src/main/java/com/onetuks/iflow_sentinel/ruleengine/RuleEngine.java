package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.ruleengine.evaluator.RuleTypeEvaluator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 파싱 모델과 유효 규칙 목록을 받아 Finding을 산출하는 파사드. rule.type()으로 evaluator를 찾아 위임하며,
 * 지원 evaluator가 없는 타입(CUSTOM_EXPRESSION 등, 이번 범위 제외)은 조용히 건너뛴다.
 */
@Component
public class RuleEngine {

    private final Map<RuleType, RuleTypeEvaluator> evaluators;

    public RuleEngine(List<RuleTypeEvaluator> evaluatorBeans) {
        this.evaluators = evaluatorBeans.stream()
                .collect(Collectors.toMap(RuleTypeEvaluator::supports, Function.identity()));
    }

    public List<FindingResult> evaluate(List<ArtifactParsedModel> models, List<EffectiveRule> rules) {
        List<FindingResult> findings = new ArrayList<>();
        for (EffectiveRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }
            RuleTypeEvaluator evaluator = evaluators.get(rule.rule().getType());
            if (evaluator == null) {
                continue;
            }
            findings.addAll(evaluator.evaluate(rule, models));
        }
        return findings;
    }
}
