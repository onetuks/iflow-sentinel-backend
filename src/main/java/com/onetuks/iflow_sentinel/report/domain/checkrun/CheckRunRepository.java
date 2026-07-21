package com.onetuks.iflow_sentinel.report.domain.checkrun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckRunRepository extends JpaRepository<CheckRun, Long> {

    List<CheckRun> findByProjectId(Long projectId);
}
