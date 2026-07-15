package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunStatus;
import java.util.Map;

public record CheckRunUpdateRequest(
    CheckRunStatus status,
    Map<String, Object> summary
) {}
