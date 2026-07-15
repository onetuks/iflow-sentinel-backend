package com.onetuks.iflow_sentinel.report.persistence;

import com.onetuks.iflow_sentinel.report.domain.finding.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingJpaRepository extends JpaRepository<Finding, Long> {
}
