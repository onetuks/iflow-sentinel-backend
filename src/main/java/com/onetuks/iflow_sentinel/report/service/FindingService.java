package com.onetuks.iflow_sentinel.report.service;

import com.onetuks.iflow_sentinel.report.domain.finding.Finding;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.report.dto.FindingCreateRequest;
import com.onetuks.iflow_sentinel.report.dto.FindingUpdateRequest;
import com.onetuks.iflow_sentinel.report.persistence.FindingJpaRepository;
import com.onetuks.iflow_sentinel.report.persistence.CheckRunJpaRepository;
import com.onetuks.iflow_sentinel.connector.persistence.ArtifactJpaRepository;
import com.onetuks.iflow_sentinel.rule.persistence.RuleJpaRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindingService {

    private final FindingJpaRepository findingRepository;
    private final CheckRunJpaRepository checkRunRepository;
    private final ArtifactJpaRepository artifactRepository;
    private final RuleJpaRepository ruleRepository;

    @Transactional
    public Finding createFinding(FindingCreateRequest request) {
        CheckRun checkRun = checkRunRepository.findById(request.checkRunId())
                .orElseThrow(NoSuchElementException::new);
        Artifact artifact = artifactRepository.findById(request.artifactId())
                .orElseThrow(NoSuchElementException::new);
        Rule rule = ruleRepository.findById(request.ruleId())
                .orElseThrow(NoSuchElementException::new);

        Finding newFinding = Finding.builder()
                .checkRun(checkRun)
                .artifact(artifact)
                .rule(rule)
                .severity(request.severity())
                .location(request.location())
                .message(request.message())
                .build();

        return findingRepository.save(newFinding);
    }

    @Transactional
    public Finding updateFinding(Long id, FindingUpdateRequest request) {
        Finding finding = findingRepository.findById(id).orElseThrow(NoSuchElementException::new);
        // Add update logic here if Entity supports it
        return findingRepository.save(finding);
    }

    @Transactional(readOnly = true)
    public Finding getFindingById(Long id) {
        return findingRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional
    public void removeFinding(Long id) {
        findingRepository.deleteById(id);
    }
}
