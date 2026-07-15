package com.onetuks.iflow_sentinel.rule.persistence;

import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleJpaRepository extends JpaRepository<Rule, Long> {
}
