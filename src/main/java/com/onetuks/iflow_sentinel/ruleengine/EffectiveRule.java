package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.rule.domain.Rule;
import com.onetuks.iflow_sentinel.rule.domain.Severity;

/**
 * 프로젝트에 적용된(ProjectRule.isEnabled=true) 규칙의 유효값. evaluator는
 * rule.target()/params()/type()/message()를 읽고, Finding의 심각도는 이 severity를 사용한다.
 */
public record EffectiveRule(Rule rule, Severity severity, boolean enabled) {
}
