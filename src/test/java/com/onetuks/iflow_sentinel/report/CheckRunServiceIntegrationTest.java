package com.onetuks.iflow_sentinel.report;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRule;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRuleRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.service.ArtifactDownloadService;
import com.onetuks.iflow_sentinel.exception.ConnectorException;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunRepository;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRunStatus;
import com.onetuks.iflow_sentinel.report.domain.finding.FindingRepository;
import com.onetuks.iflow_sentinel.report.dto.CheckRunResponse;
import com.onetuks.iflow_sentinel.report.service.CheckRunService;
import com.onetuks.iflow_sentinel.rule.domain.Rule;
import com.onetuks.iflow_sentinel.rule.domain.RuleRepository;
import com.onetuks.iflow_sentinel.rule.domain.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.Severity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Connector 다운로드만 목을 씌우고, Parser·Rule Engine·Repository는 실제로 동작시켜
 * Run(CheckRun) 전체 오케스트레이션을 H2 인메모리 DB로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class CheckRunServiceIntegrationTest {

    @TestConfiguration
    static class MockArtifactDownloadConfig {
        @Bean
        @Primary
        ArtifactDownloadService artifactDownloadService() {
            return mock(ArtifactDownloadService.class);
        }
    }

    @Autowired
    private CheckRunService checkRunService;
    @Autowired
    private ArtifactDownloadService artifactDownloadService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private IntegrationPackageRepository packageRepository;
    @Autowired
    private ArtifactRepository artifactRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private ProjectRuleRepository projectRuleRepository;
    @Autowired
    private CheckRunRepository checkRunRepository;
    @Autowired
    private FindingRepository findingRepository;

    private Project project;
    private IntegrationPackage integrationPackage;
    private Artifact artifact;

    @BeforeEach
    void setUp() {
        // artifactDownloadService는 @Primary 목으로 스프링 컨텍스트에 싱글턴 등록되어 테스트 메서드 간
        // 공유되므로, 이전 테스트의 스터빙(when(...).thenThrow 등)이 남아있지 않도록 매번 리셋한다.
        org.mockito.Mockito.reset(artifactDownloadService);

        String unique = String.valueOf(System.nanoTime());
        project = projectRepository.save(Project.builder().name("Test Project").build());
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .project(project)
                .name("Test Tenant")
                .odataUrl("https://tenant.example.com/api/v1")
                .tokenUrl("https://tenant.example.com/oauth/token")
                .platformType(TenantPlatform.CLOUD_FOUNDRY)
                .authType(TenantAuthType.OAUTH2_CLIENT_CREDENTIALS)
                .clientId("client-id")
                .clientSecret("client-secret")
                .build());
        integrationPackage = packageRepository.save(IntegrationPackage.builder()
                .tenant(tenant).sapPackageId("PKG1-" + unique).name("Package One").build());
        artifact = artifactRepository.save(Artifact.builder()
                .integrationPackage(integrationPackage)
                .sapArtifactId("GMES_GQMS_EA_PQCRESULT_01-" + unique)
                .name("GMES_GQMS_EA_PQCRESULT_01")
                .version("1.0.5")
                .type(ArtifactType.IFLOW)
                .build());

        Rule rule = ruleRepository.save(Rule.builder()
                .ruleKey("required-error-handler-" + unique)
                .isGlobal(true)
                .type(RuleType.REQUIRED_ERROR_HANDLER)
                .severity(Severity.FAIL)
                .target(Map.of())
                .params(Map.of())
                .message("예외 처리 서브프로세스가 필요합니다.")
                .enabled(true)
                .build());
        projectRuleRepository.save(ProjectRule.builder().project(project).rule(rule).isEnabled(true).build());
    }

    @Test
    void runDownloadsParsesEvaluatesAndPersistsFindings() {
        when(artifactDownloadService.downloadZip(any())).thenReturn(sampleZipBytes());

        CheckRunResponse response = checkRunService.run(project.getId(), artifact.getId());

        assertThat(response.status()).isEqualTo(CheckRunStatus.COMPLETED);
        // 실제 픽스처는 exceptionSubprocesses가 0개이므로 required-error-handler가 정확히 1건 위반된다.
        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().get(0).severity()).isEqualTo(Severity.FAIL);

        assertThat(checkRunRepository.findById(response.id()).orElseThrow().getStatus())
                .isEqualTo(CheckRunStatus.COMPLETED);
        assertThat(findingRepository.findByCheckRunId(response.id())).hasSize(1);
    }

    @Test
    void runBatchEvaluatesAllArtifactsInPackageTogether() {
        when(artifactDownloadService.downloadZip(any())).thenReturn(sampleZipBytes());

        CheckRunResponse response = checkRunService.runBatch(project.getId(), integrationPackage.getId());

        assertThat(response.status()).isEqualTo(CheckRunStatus.COMPLETED);
        assertThat(response.findings()).hasSize(1);
        assertThat(findingRepository.findByCheckRunId(response.id())).hasSize(1);
    }

    @Test
    void downloadFailureMarksCheckRunFailedAndRethrows() {
        when(artifactDownloadService.downloadZip(any())).thenThrow(new ConnectorException("다운로드 실패", 502));

        assertThatThrownBy(() -> checkRunService.run(project.getId(), artifact.getId()))
                .isInstanceOf(ConnectorException.class);

        // FAILED 상태로 CheckRun 자체는 남아 있어야 한다(예외가 전파되어도 저장은 유지).
        assertThat(checkRunRepository.findAll())
                .anySatisfy(cr -> assertThat(cr.getStatus()).isEqualTo(CheckRunStatus.FAILED));
    }

    private static byte[] sampleZipBytes() {
        try (InputStream in = CheckRunServiceIntegrationTest.class
                .getResourceAsStream("/parser/GMES_GQMS_EA_PQCRESULT_01.zip")) {
            if (in == null) {
                throw new IllegalStateException("테스트 픽스처를 찾을 수 없습니다: /parser/GMES_GQMS_EA_PQCRESULT_01.zip");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
