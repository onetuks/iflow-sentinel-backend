package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.Severity;

/**
 * BindingOverride가 적용된 후의 규칙 유효값. evaluator는 rule.target()/params()/type()/message()를 읽되,
 * Finding의 심각도는 rule.severity()가 아니라 이 severity(오버라이드 반영값)를 사용해야 한다.
 */
public record EffectiveRule(Rule rule, Severity severity, boolean enabled) {
}
