package com.onetuks.iflow_sentinel.checkrun;

import com.onetuks.iflow_sentinel.checkrun.dto.CheckRunResponse;
import com.onetuks.iflow_sentinel.connector.ArtifactDownloadService;
import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.domain.binding.Binding;
import com.onetuks.iflow_sentinel.domain.binding.BindingRepository;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRunRepository;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRunStatus;
import com.onetuks.iflow_sentinel.domain.finding.Finding;
import com.onetuks.iflow_sentinel.domain.finding.FindingRepository;
import com.onetuks.iflow_sentinel.domain.rule.Severity;
import com.onetuks.iflow_sentinel.parser.ParserFacade;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import com.onetuks.iflow_sentinel.ruleengine.RuleEngine;
import com.onetuks.iflow_sentinel.ruleengine.RuleResolutionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Run(CHK-001): Connector로 아티팩트를 내려받아 Parser로 파싱하고, Binding에 적용된 유효 규칙으로
 * Rule Engine을 평가해 Finding을 저장한다. 이번 범위는 단일 아티팩트 검사만 다룬다.
 * 의도적으로 @Transactional을 두지 않는다 — 3~5단계 중 예외가 나도 이미 RUNNING으로 저장된 CheckRun을
 * FAILED로 갱신해 남겨야 하므로, 각 repository.save() 호출이 독립적으로 커밋되어야 한다.
 */
@Service
public class CheckRunService {

    private final BindingRepository bindingRepository;
    private final ArtifactRepository artifactRepository;
    private final CheckRunRepository checkRunRepository;
    private final FindingRepository findingRepository;
    private final ArtifactDownloadService artifactDownloadService;
    private final ParserFacade parserFacade;
    private final RuleResolutionService ruleResolutionService;
    private final RuleEngine ruleEngine;

    public CheckRunService(
            BindingRepository bindingRepository,
            ArtifactRepository artifactRepository,
            CheckRunRepository checkRunRepository,
            FindingRepository findingRepository,
            ArtifactDownloadService artifactDownloadService,
            ParserFacade parserFacade,
            RuleResolutionService ruleResolutionService,
            RuleEngine ruleEngine
    ) {
        this.bindingRepository = bindingRepository;
        this.artifactRepository = artifactRepository;
        this.checkRunRepository = checkRunRepository;
        this.findingRepository = findingRepository;
        this.artifactDownloadService = artifactDownloadService;
        this.parserFacade = parserFacade;
        this.ruleResolutionService = ruleResolutionService;
        this.ruleEngine = ruleEngine;
    }

    public CheckRunResponse run(Long bindingId, Long artifactId) {
        Binding binding = bindingRepository.findById(bindingId)
                .orElseThrow(() -> new NoSuchElementException("바인딩을 찾을 수 없습니다: " + bindingId));
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new NoSuchElementException("아티팩트를 찾을 수 없습니다: " + artifactId));

        CheckRun checkRun = checkRunRepository.save(CheckRun.builder()
                .project(binding.getProject())
                .ruleset(binding.getRuleset())
                .startedAt(LocalDateTime.now())
                .status(CheckRunStatus.RUNNING)
                .build());

        try {
            byte[] zipBytes = artifactDownloadService.downloadZip(artifact);
            ParsedModel parsedModel = parserFacade.parse(zipBytes);

            List<EffectiveRule> effectiveRules = ruleResolutionService.resolveEffectiveRules(binding.getId());
            List<FindingResult> findingResults = ruleEngine.evaluate(
                    List.of(new ArtifactParsedModel(artifact, parsedModel)), effectiveRules);

            List<Finding> findings = findingResults.stream()
                    .map(result -> Finding.builder()
                            .checkRun(checkRun)
                            .artifact(result.artifact())
                            .rule(result.rule())
                            .severity(result.severity())
                            .location(result.location())
                            .message(result.message())
                            .build())
                    .toList();
            findingRepository.saveAll(findings);

            checkRun.updateStatus(CheckRunStatus.COMPLETED, summarize(findingResults));
            checkRunRepository.save(checkRun);

            return CheckRunResponse.from(checkRun, findings);
        } catch (RuntimeException e) {
            checkRun.updateStatus(CheckRunStatus.FAILED, Map.of("error", String.valueOf(e.getMessage())));
            checkRunRepository.save(checkRun);
            throw e;
        }
    }

    public CheckRunResponse get(Long id) {
        CheckRun checkRun = checkRunRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("검사 이력을 찾을 수 없습니다: " + id));
        return CheckRunResponse.from(checkRun, findingRepository.findByCheckRunId(id));
    }

    public List<CheckRunResponse> list(Long projectId) {
        return checkRunRepository.findByProjectId(projectId).stream()
                .map(checkRun -> CheckRunResponse.from(checkRun, List.of()))
                .toList();
    }

    private Map<String, Object> summarize(List<FindingResult> findingResults) {
        long fail = findingResults.stream().filter(r -> r.severity() == Severity.FAIL).count();
        long warn = findingResults.stream().filter(r -> r.severity() == Severity.WARN).count();
        long info = findingResults.stream().filter(r -> r.severity() == Severity.INFO).count();
        return Map.of("fail", fail, "warn", warn, "info", info, "total", (long) findingResults.size());
    }
}
