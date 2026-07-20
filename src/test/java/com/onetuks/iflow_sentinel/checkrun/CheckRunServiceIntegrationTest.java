package com.onetuks.iflow_sentinel.checkrun;

import com.onetuks.iflow_sentinel.checkrun.dto.CheckRunResponse;
import com.onetuks.iflow_sentinel.connector.ArtifactDownloadService;
import com.onetuks.iflow_sentinel.connector.ConnectorException;
import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.domain.binding.Binding;
import com.onetuks.iflow_sentinel.domain.binding.BindingRepository;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRunRepository;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRunStatus;
import com.onetuks.iflow_sentinel.domain.finding.FindingRepository;
import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.domain.project.Project;
import com.onetuks.iflow_sentinel.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.domain.rule.Severity;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import com.onetuks.iflow_sentinel.domain.ruleset.RulesetRepository;
import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.domain.tenant.TenantRepository;
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
    private RulesetRepository rulesetRepository;
    @Autowired
    private BindingRepository bindingRepository;
    @Autowired
    private CheckRunRepository checkRunRepository;
    @Autowired
    private FindingRepository findingRepository;

    private Artifact artifact;
    private Binding binding;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.nanoTime());
        Project project = projectRepository.save(Project.builder().name("Test Project").build());
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
        IntegrationPackage integrationPackage = packageRepository.save(IntegrationPackage.builder()
                .tenant(tenant).sapPackageId("PKG1-" + unique).name("Package One").build());
        artifact = artifactRepository.save(Artifact.builder()
                .integrationPackage(integrationPackage)
                .sapArtifactId("GMES_GQMS_EA_PQCRESULT_01-" + unique)
                .name("GMES_GQMS_EA_PQCRESULT_01")
                .version("1.0.5")
                .type(ArtifactType.IFLOW)
                .build());

        Ruleset ruleset = Ruleset.builder().rulesetKey("rs-" + unique).version("1.0").description("test ruleset").build();
        Rule rule = Rule.builder()
                .ruleKey("required-error-handler-" + unique)
                .type(RuleType.REQUIRED_ERROR_HANDLER)
                .severity(Severity.FAIL)
                .target(Map.of())
                .params(Map.of())
                .message("예외 처리 서브프로세스가 필요합니다.")
                .enabled(true)
                .build();
        ruleset.addRule(rule);
        ruleset = rulesetRepository.save(ruleset);

        binding = bindingRepository.save(Binding.builder().project(project).ruleset(ruleset).build());
    }

    @Test
    void runDownloadsParsesEvaluatesAndPersistsFindings() {
        when(artifactDownloadService.downloadZip(any())).thenReturn(sampleZipBytes());

        CheckRunResponse response = checkRunService.run(binding.getId(), artifact.getId());

        assertThat(response.status()).isEqualTo(CheckRunStatus.COMPLETED);
        // 실제 픽스처는 exceptionSubprocesses가 0개이므로 required-error-handler가 정확히 1건 위반된다.
        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().get(0).severity()).isEqualTo(Severity.FAIL);

        assertThat(checkRunRepository.findById(response.id()).orElseThrow().getStatus()).isEqualTo(CheckRunStatus.COMPLETED);
        assertThat(findingRepository.findByCheckRunId(response.id())).hasSize(1);
    }

    @Test
    void downloadFailureMarksCheckRunFailedAndRethrows() {
        when(artifactDownloadService.downloadZip(any())).thenThrow(new ConnectorException("다운로드 실패", 502));

        assertThatThrownBy(() -> checkRunService.run(binding.getId(), artifact.getId()))
                .isInstanceOf(ConnectorException.class);

        // FAILED 상태로 CheckRun 자체는 남아 있어야 한다(예외가 전파되어도 저장은 유지).
        assertThat(checkRunRepository.findAll())
                .anySatisfy(cr -> assertThat(cr.getStatus()).isEqualTo(CheckRunStatus.FAILED));
    }

    private static byte[] sampleZipBytes() {
        try (InputStream in = CheckRunServiceIntegrationTest.class.getResourceAsStream("/parser/GMES_GQMS_EA_PQCRESULT_01.zip")) {
            if (in == null) {
                throw new IllegalStateException("테스트 픽스처를 찾을 수 없습니다: /parser/GMES_GQMS_EA_PQCRESULT_01.zip");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
