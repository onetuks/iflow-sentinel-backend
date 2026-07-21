package com.onetuks.iflow_sentinel.report.service;

import com.onetuks.iflow_sentinel.report.domain.finding.FindingRepository;
import com.onetuks.iflow_sentinel.report.dto.FindingResponse;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;
import org.springframework.stereotype.Service;

import java.util.List;

/** RPT-002/003: 위반 상세 조회 및 심각도·아티팩트·규칙 기준 필터링. */
@Service
public class FindingService {

    private final FindingRepository findingRepository;

    public FindingService(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    public List<FindingResponse> search(Long checkRunId, Severity severity, Long ruleId, Long artifactId) {
        return findingRepository.search(checkRunId, severity, ruleId, artifactId).stream()
                .map(FindingResponse::from)
                .toList();
    }
}
