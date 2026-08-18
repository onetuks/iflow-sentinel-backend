package com.onetuks.iflow_sentinel.reprocess;

import com.onetuks.iflow_sentinel.reprocess.domain.ConfidenceLevel;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageMappingRepository;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.StorageMappingDto;
import com.onetuks.iflow_sentinel.reprocess.service.StorageMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StorageMappingServiceTest {

    @Autowired
    private StorageMappingService storageMappingService;

    @Autowired
    private StorageMappingRepository storageMappingRepository;

    private Long tenantId = 1L;
    private Long artifactId = 100L;

    @BeforeEach
    void setUp() {
        storageMappingRepository.deleteAll();
    }

    @Test
    @DisplayName("자동 추출(AUTO_PARSED) 매핑을 저장하고 조회한다")
    void saveOrUpdateMapping_autoParsed() {
        // when
        StorageMappingDto dto = storageMappingService.saveOrUpdateMapping(
                tenantId, artifactId, StorageType.DATASTORE, "AutoDataStore", 90, ConfidenceLevel.AUTO_PARSED
        );

        // then
        assertThat(dto.storageName()).isEqualTo("AutoDataStore");
        assertThat(dto.confidenceLevel()).isEqualTo(ConfidenceLevel.AUTO_PARSED);

        Optional<StorageMappingDto> retrieved = storageMappingService.getStorageMapping(tenantId, artifactId, StorageType.DATASTORE);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().storageName()).isEqualTo("AutoDataStore");
    }

    @Test
    @DisplayName("수동 오버라이드(MANUAL) 매핑은 자동 추출 매핑보다 최우선 적용되며 자동 매핑으로 덮어써지지 않는다")
    void manualMapping_overridesAutoParsed() {
        // given: 수동 매핑 등록
        storageMappingService.saveOrUpdateManualMapping(tenantId, artifactId, StorageType.DATASTORE, "ManualDataStore", 60);

        // when: 자동 매핑 덮어쓰기 시도
        StorageMappingDto result = storageMappingService.saveOrUpdateMapping(
                tenantId, artifactId, StorageType.DATASTORE, "AutoDataStoreAttempt", 90, ConfidenceLevel.AUTO_PARSED
        );

        // then: 기존 수동 값이 유지되어야 함
        assertThat(result.storageName()).isEqualTo("ManualDataStore");
        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.MANUAL);

        List<StorageMappingDto> mappings = storageMappingService.getStorageMappings(tenantId, artifactId);
        assertThat(mappings).hasSize(1);
        assertThat(mappings.get(0).storageName()).isEqualTo("ManualDataStore");
    }
}
