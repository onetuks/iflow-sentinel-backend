package com.onetuks.iflow_sentinel.report.persistence;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckRunJpaRepository extends JpaRepository<CheckRun, Long> {
}
