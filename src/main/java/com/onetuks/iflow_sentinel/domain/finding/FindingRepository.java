package com.onetuks.iflow_sentinel.domain.finding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByCheckRunId(Long checkRunId);
}
