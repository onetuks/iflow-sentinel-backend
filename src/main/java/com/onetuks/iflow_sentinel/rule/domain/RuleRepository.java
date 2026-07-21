package com.onetuks.iflow_sentinel.rule.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RuleRepository extends JpaRepository<Rule, Long> {

    @Query("select r from Rule r where r.isGlobal = true or r.customProject.id = :projectId")
    List<Rule> findByIsGlobalTrueOrCustomProjectId(@Param("projectId") Long projectId);
}
