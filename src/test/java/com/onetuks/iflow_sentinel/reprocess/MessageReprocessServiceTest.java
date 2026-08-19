package com.onetuks.iflow_sentinel.reprocess;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.reprocess.domain.ConfidenceLevel;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageBodyResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessRequest;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessResult;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService;
import com.onetuks.iflow_sentinel.reprocess.service.StorageMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MessageReprocessServiceTest {

    @Autowired
    private MessageReprocessService messageReprocessService;

    @Autowired
    private StorageMappingService storageMappingService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private IntegrationPackageRepository packageRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    private Tenant tenant;
    private IntegrationPackage integrationPackage;
    private Artifact artifact;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant")
                .odataUrl("https://test.hana.ondemand.com/api/v1")
                .tokenUrl("https://test.hana.ondemand.com/oauth/token")
                .platformType(com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform.CLOUD_FOUNDRY)
                .authType(com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType.OAUTH2_CLIENT_CREDENTIALS)
                .clientId("client-id")
                .clientSecret("client-secret")
                .build());

        integrationPackage = packageRepository.save(IntegrationPackage.builder()
                .tenant(tenant)
                .sapPackageId("pkg_test")
                .name("Test Package")
                .build());

        artifact = artifactRepository.save(Artifact.builder()
                .integrationPackage(integrationPackage)
                .sapArtifactId("iflow_test")
                .name("Test iFlow")
                .version("1.0.0")
                .type(ArtifactType.IFLOW)
                .reprocessSupportType(ReprocessSupportType.BOTH)
                .build());

        storageMappingService.saveOrUpdateMapping(
                tenant.getId(), artifact.getId(), StorageType.DATASTORE, "DS_TEST", 30, ConfidenceLevel.AUTO_PARSED
        );
        storageMappingService.saveOrUpdateMapping(
                tenant.getId(), artifact.getId(), StorageType.JMS, "JMS_QUEUE_TEST", null, ConfidenceLevel.AUTO_PARSED
        );
    }

    @Test
    @DisplayName("아티팩트의 재처리 지원 유형을 조회한다")
    void getReprocessSupportType() {
        ReprocessSupportType supportType = messageReprocessService.getReprocessSupportType(artifact.getId());
        assertThat(supportType).isEqualTo(ReprocessSupportType.BOTH);
    }

    @Test
    @DisplayName("MPL 실패 목록을 조회하여 DTO로 반환한다 (OData 연결 오류 발생 시에도 안전한 폴백 제공)")
    void getMplFailures() {
        List<MplFailureResponse> failures = messageReprocessService.getMplFailures(tenant.getId(), String.valueOf(artifact.getId()), 10);
        assertThat(failures).isNotNull();
    }

    @Test
    @DisplayName("JMS 메시지 바디 및 보존 상태를 정상적으로 연산하여 반환한다")
    void getMessageBody_jms() {
        MessageBodyResponse response = messageReprocessService.getMessageBody(
                tenant.getId(), artifact.getId(), "MSG_12345", StorageType.JMS
        );

        assertThat(response).isNotNull();
        assertThat(response.messageId()).isEqualTo("MSG_12345");
        assertThat(response.storageName()).isEqualTo("JMS_QUEUE_TEST");
        assertThat(response.deepLinkUrl()).contains("JmsQueues?queue=JMS_QUEUE_TEST");
    }

    @Test
    @DisplayName("DataStore 메시지 바디 조회 시 OData 연동 실패 시 ConnectorException 예외를 던진다")
    void getMessageBody_datastore_fail() {
        assertThatThrownBy(() -> messageReprocessService.getMessageBody(
                tenant.getId(), artifact.getId(), "MSG_12345", StorageType.DATASTORE
        )).isInstanceOf(com.onetuks.iflow_sentinel.exception.ConnectorException.class);
    }

    @Test
    @DisplayName("JMS 메시지 재처리 실행 요청 시 딥링크 및 반자동 안내 메시지를 반환한다")
    void reprocessMessage_jms() {
        MessageReprocessRequest request = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_JMS_1", StorageType.JMS, "JMS_QUEUE_TEST"
        );

        MessageReprocessResult result = messageReprocessService.reprocessMessage(request);

        assertThat(result.success()).isTrue();
        assertThat(result.deepLinkUrl()).contains("JmsQueues?queue=JMS_QUEUE_TEST");
        assertThat(result.statusMessage()).contains("Manage Queues 바로가기 링크");
    }
}
