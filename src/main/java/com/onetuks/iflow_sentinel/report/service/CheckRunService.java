package com.onetuks.iflow_sentinel.report.service;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.service.ArtifactDownloadService;
import com.onetuks.iflow_sentinel.parser.ParserFacade;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.parser.util.ReprocessSupportCalculator;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunRepository;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunStatus;
import com.onetuks.iflow_sentinel.report.domain.finding.Finding;
import com.onetuks.iflow_sentinel.report.domain.finding.FindingRepository;
import com.onetuks.iflow_sentinel.report.dto.CheckRunResponse;
import com.onetuks.iflow_sentinel.reprocess.domain.ConfidenceLevel;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.service.StorageMappingService;
import com.onetuks.iflow_sentinel.rule.domain.Severity;
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
 * Run(CHK-001/002): Connector로 아티팩트를 내려받아 Parser로 파싱하고, 프로젝트에 적용된(ProjectRule)
 * 유효 규칙으로 Rule Engine을 평가해 Finding을 저장한다. run()은 단일 아티팩트, runBatch()는 패키지 하위
 * 전체 아티팩트를 함께 평가한다 — CROSS 규칙(예: ProcessDirect 짝 검증)은 여러 아티팩트를 동시에 봐야
 * 판정 가능하므로 배치 경로에서만 의미가 있다.
 * 의도적으로 @Transactional을 두지 않는다 — 중간 단계에서 예외가 나도 이미 RUNNING으로 저장된 CheckRun을
 * FAILED로 갱신해 남겨야 하므로, 각 repository.save() 호출이 독립적으로 커밋되어야 한다.
 */
@Service
public class CheckRunService {

        private final ProjectRepository projectRepository;
        private final ArtifactRepository artifactRepository;
        private final CheckRunRepository checkRunRepository;
        private final FindingRepository findingRepository;
        private final ArtifactDownloadService artifactDownloadService;
        private final ParserFacade parserFacade;
        private final RuleResolutionService ruleResolutionService;
        private final RuleEngine ruleEngine;
        private final StorageMappingService storageMappingService;

        public CheckRunService(
                        ProjectRepository projectRepository,
                        ArtifactRepository artifactRepository,
                        CheckRunRepository checkRunRepository,
                        FindingRepository findingRepository,
                        ArtifactDownloadService artifactDownloadService,
                        ParserFacade parserFacade,
                        RuleResolutionService ruleResolutionService,
                        RuleEngine ruleEngine,
                        StorageMappingService storageMappingService) {
                this.projectRepository = projectRepository;
                this.artifactRepository = artifactRepository;
                this.checkRunRepository = checkRunRepository;
                this.findingRepository = findingRepository;
                this.artifactDownloadService = artifactDownloadService;
                this.parserFacade = parserFacade;
                this.ruleResolutionService = ruleResolutionService;
                this.ruleEngine = ruleEngine;
                this.storageMappingService = storageMappingService;
        }

        public CheckRunResponse run(Long projectId, String artifactId) {
                Project project = findProject(projectId);
                Artifact artifact = artifactRepository.findWithPackageAndTenantById(artifactId)
                                .orElseThrow(() -> new NoSuchElementException("아티팩트를 찾을 수 없습니다: " + artifactId));

                CheckRun checkRun = startCheckRun(project);
                try {
                        ArtifactParsedModel model = downloadAndParse(artifact);
                        return evaluateAndComplete(checkRun, project, List.of(model));
                } catch (RuntimeException e) {
                        return failCheckRun(checkRun, e);
                }
        }

        public CheckRunResponse runBatch(Long projectId, Long integrationPackageId) {
                Project project = findProject(projectId);
                List<Artifact> artifacts = artifactRepository.findWithPackageAndTenantByIntegrationPackageId(integrationPackageId);

                CheckRun checkRun = startCheckRun(project);
                try {
                        List<ArtifactParsedModel> models = artifacts.stream()
                                        .map(this::downloadAndParse)
                                        .toList();
                        return evaluateAndComplete(checkRun, project, models);
                } catch (RuntimeException e) {
                        return failCheckRun(checkRun, e);
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

        private Project findProject(Long projectId) {
                return projectRepository.findById(projectId)
                                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + projectId));
        }

        private CheckRun startCheckRun(Project project) {
                return checkRunRepository.save(CheckRun.builder()
                                .project(project)
                                .startedAt(LocalDateTime.now())
                                .status(CheckRunStatus.RUNNING)
                                .build());
        }

        private ArtifactParsedModel downloadAndParse(Artifact artifact) {
                byte[] zipBytes = artifactDownloadService.downloadZip(artifact);
                ParsedModel parsedModel = parserFacade.parse(zipBytes);

                if (parsedModel != null && parsedModel.iflow() != null) {
                        ReprocessSupportType supportType =
                                        ReprocessSupportCalculator.calculateSupportType(parsedModel.iflow());
                        artifact.updateReprocessSupportType(supportType);
                        artifactRepository.save(artifact);

                        Long tenantId = artifact.getIntegrationPackage() != null && artifact.getIntegrationPackage().getTenant() != null
                                        ? artifact.getIntegrationPackage().getTenant().getId()
                                        : null;

                        if (tenantId != null) {
                                ReprocessSupportCalculator.extractDataStoreInfo(parsedModel.iflow())
                                                .ifPresent(info -> storageMappingService.saveOrUpdateMapping(
                                                                tenantId, artifact.getId(),
                                                                StorageType.DATASTORE,
                                                                info.name(), info.expireDays(),
                                                                ConfidenceLevel.AUTO_PARSED));

                                ReprocessSupportCalculator.extractJmsQueueName(parsedModel.iflow())
                                                .ifPresent(queueName -> storageMappingService.saveOrUpdateMapping(
                                                                tenantId, artifact.getId(),
                                                                StorageType.JMS,
                                                                queueName, null,
                                                                ConfidenceLevel.AUTO_PARSED));
                        }
                }

                return new ArtifactParsedModel(artifact, parsedModel);
        }

        private CheckRunResponse evaluateAndComplete(CheckRun checkRun, Project project,
                        List<ArtifactParsedModel> models) {
                List<EffectiveRule> effectiveRules = ruleResolutionService.resolveEffectiveRules(project.getId());
                List<FindingResult> findingResults = ruleEngine.evaluate(models, effectiveRules);

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
        }

        private CheckRunResponse failCheckRun(CheckRun checkRun, RuntimeException e) {
                checkRun.updateStatus(CheckRunStatus.FAILED, Map.of("error", String.valueOf(e.getMessage())));
                checkRunRepository.save(checkRun);
                throw e;
        }

        private Map<String, Object> summarize(List<FindingResult> findingResults) {
                long fail = findingResults.stream().filter(r -> r.severity() == Severity.FAIL).count();
                long warn = findingResults.stream().filter(r -> r.severity() == Severity.WARN).count();
                long info = findingResults.stream().filter(r -> r.severity() == Severity.INFO).count();
                return Map.of("fail", fail, "warn", warn, "info", info, "total", (long) findingResults.size());
        }
}
