package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.Severity;

/** Rule Engine이 산출한 규칙 위반 하나. CheckRunService가 이를 Finding 엔티티로 영속화한다. */
public record FindingResult(Artifact artifact, Rule rule, Severity severity, String location, String message) {
}
