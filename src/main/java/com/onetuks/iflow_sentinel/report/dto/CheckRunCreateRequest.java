package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunStatus;
import java.time.LocalDateTime;

public record CheckRunCreateRequest(
    Long projectId,
    LocalDateTime startedAt,
    CheckRunStatus status
) {}
