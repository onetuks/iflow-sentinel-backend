package com.onetuks.iflow_sentinel.reprocess;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.reprocess.domain.ConfidenceLevel;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessStatus;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageBodyResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessRequest;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessResult;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.ReprocessHistoryResponse;
import com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService;
import com.onetuks.iflow_sentinel.reprocess.service.StorageMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
                .interfaceUrl("https://test-rt.cfapps.eu10.hana.ondemand.com")
                .interfaceAuthType(com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType.BASIC)
                .interfaceUsername("iflow-user")
                .interfacePassword("iflow-pass")
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
        List<MplFailureResponse> failures = messageReprocessService.getMplFailures(tenant.getId(), artifact.getId(), 10);
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
    @DisplayName("JMS 메시지 재처리 실행 요청 시 딥링크 및 반자동 안내 메시지를 반환하고 히스토리를 저장한다")
    void reprocessMessage_jms() {
        MessageReprocessRequest request = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_JMS_1", StorageType.JMS, "JMS_QUEUE_TEST", "USER_ADMIN"
        );

        MessageReprocessResult result = messageReprocessService.reprocessMessage(request);

        assertThat(result.success()).isTrue();
        assertThat(result.historyId()).isNotNull();
        assertThat(result.deepLinkUrl()).contains("JmsQueues?queue=JMS_QUEUE_TEST");
        assertThat(result.statusMessage()).contains("Manage Queues 바로가기 링크");

        // 히스토리 단건 조회 확인
        ReprocessHistoryResponse history = messageReprocessService.getReprocessHistory(result.historyId());
        assertThat(history.messageId()).isEqualTo("MSG_JMS_1");
        assertThat(history.status()).isEqualTo(ReprocessStatus.SUCCESS);
        assertThat(history.reprocessedBy()).isEqualTo("USER_ADMIN");
        assertThat(history.artifactName()).isEqualTo("Test iFlow");
    }

    @Test
    @DisplayName("DataStore 메시지 재처리 시 런타임 엔드포인트 URL을 찾지 못하면 404 실패 결과와 안내 메시지를 반환한다")
    void reprocessMessage_datastore_no_endpoint_url() {
        MessageReprocessRequest request = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_DS_NO_URL", StorageType.DATASTORE, "DS_TEST", "TEST_USER"
        );

        MessageReprocessResult result = messageReprocessService.reprocessMessage(request);

        assertThat(result.success()).isFalse();
        assertThat(result.historyId()).isNotNull();
        assertThat(result.httpStatusCode()).isEqualTo(404);
        assertThat(result.statusMessage()).contains("호출 가능한 인터페이스 엔드포인트 URL을 찾을 수 없습니다");

        // 히스토리 조회
        ReprocessHistoryResponse history = messageReprocessService.getReprocessHistory(result.historyId());
        assertThat(history.status()).isEqualTo(ReprocessStatus.FAILED);
        assertThat(history.httpStatusCode()).isEqualTo(404);
        assertThat(history.statusMessage()).contains("호출 가능한 인터페이스 엔드포인트 URL을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("DataStore 메시지 재처리 시 엔드포인트 URL이 명시되었으나 호출 실패 시 FAILED 상태로 히스토리가 기록된다")
    void reprocessMessage_datastore_explicit_url_fail() {
        MessageReprocessRequest request = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_DS_UNKNOWN", StorageType.DATASTORE, "DS_TEST", "TEST_USER",
                "{\"sample\":\"payload\"}", "https://invalid.endpoint.url/api"
        );

        MessageReprocessResult result = messageReprocessService.reprocessMessage(request);

        assertThat(result.success()).isFalse();
        assertThat(result.historyId()).isNotNull();
        assertThat(result.statusMessage()).contains("인터페이스 직접 호출 재처리 실패");

        // 히스토리 조회
        ReprocessHistoryResponse history = messageReprocessService.getReprocessHistory(result.historyId());
        assertThat(history.status()).isEqualTo(ReprocessStatus.FAILED);
        assertThat(history.statusMessage()).contains("인터페이스 직접 호출 재처리 실패");
    }

    @Test
    @DisplayName("인터페이스 엔드포인트 URL 해석 로직이 올바르게 동작한다")
    void resolveInterfaceEndpointUrl() {
        // 1. 요청에 지정된 endpointUrl 우선
        String customUrl = "https://custom.endpoint/api";
        String resolved1 = messageReprocessService.resolveInterfaceEndpointUrl(tenant, artifact, customUrl);
        assertThat(resolved1).isEqualTo(customUrl);

        // 2. 배포된 아티팩트 엔드포인트가 없는 경우 null 반환
        String resolved2 = messageReprocessService.resolveInterfaceEndpointUrl(tenant, artifact, null);
        assertThat(resolved2).isNull();

        // 3. 아티팩트 정보가 없는 경우 null 반환
        String resolved3 = messageReprocessService.resolveInterfaceEndpointUrl(tenant, null, null);
        assertThat(resolved3).isNull();
    }

    @Test
    @DisplayName("재처리 히스토리 목록 검색 및 필터링이 정상 작동한다")
    void getReprocessHistories() {
        MessageReprocessRequest request1 = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_HIST_1", StorageType.JMS, "JMS_QUEUE_TEST", "USER_1"
        );
        MessageReprocessRequest request2 = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_HIST_2", StorageType.JMS, "JMS_QUEUE_TEST", "USER_2"
        );

        messageReprocessService.reprocessMessage(request1);
        messageReprocessService.reprocessMessage(request2);

        List<ReprocessHistoryResponse> allHistories = messageReprocessService.getReprocessHistories(
                tenant.getId(), artifact.getId(), null, null
        );
        assertThat(allHistories).hasSizeGreaterThanOrEqualTo(2);

        List<ReprocessHistoryResponse> filtered = messageReprocessService.getReprocessHistories(
                tenant.getId(), artifact.getId(), "HIST_1", ReprocessStatus.SUCCESS
        );
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).messageId()).isEqualTo("MSG_HIST_1");
    }

    @Test
    @DisplayName("재처리 히스토리를 삭제한다")
    void deleteReprocessHistory() {
        MessageReprocessRequest request = new MessageReprocessRequest(
                tenant.getId(), artifact.getId(), "MSG_DEL_1", StorageType.JMS, "JMS_QUEUE_TEST", "USER_1"
        );
        MessageReprocessResult result = messageReprocessService.reprocessMessage(request);

        messageReprocessService.deleteReprocessHistory(result.historyId());

        assertThatThrownBy(() -> messageReprocessService.getReprocessHistory(result.historyId()))
                .isInstanceOf(NoSuchElementException.class);
    }
}
