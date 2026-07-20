package com.onetuks.iflow_sentinel.domain.ruleset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RulesetRepository extends JpaRepository<Ruleset, Long> {

    Optional<Ruleset> findByRulesetKey(String rulesetKey);
}
