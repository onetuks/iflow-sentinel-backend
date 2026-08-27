package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.LogLevel;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantLogLevelSetting;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantLogLevelSettingRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.TenantLogLevelResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 테넌트에 배포된 아티팩트 전체의 MPL 로그 레벨을 일괄 변경/재적용한다. 관리자의 수동 API 호출과
 * 10분 주기 스케줄러가 동일한 적용 로직({@link #applyLogLevelToAllArtifacts})을 공유한다.
 * 아티팩트별 SAP 호출은 서로 독립적인 블로킹 I/O이므로 가상 스레드로 동시에 실행해 지연 시간을 줄인다.
 */
@Service
public class TenantLogLevelService {

    private static final Logger log = LoggerFactory.getLogger(TenantLogLevelService.class);
    private static final String STARTED_STATUS = "STARTED";
    /** SAP가 HTTP/2 커넥션당 허용하는 동시 스트림 수를 넘지 않도록 실제 동시 SAP 호출 수를 제한한다. */
    private static final int MAX_CONCURRENT_SAP_CALLS = 10;

    private final TenantRepository tenantRepository;
    private final TenantLogLevelSettingRepository settingRepository;
    private final SapODataClient odataClient;

    public TenantLogLevelService(TenantRepository tenantRepository,
            TenantLogLevelSettingRepository settingRepository, SapODataClient odataClient) {
        this.tenantRepository = tenantRepository;
        this.settingRepository = settingRepository;
        this.odataClient = odataClient;
    }

    /** 원하는 로그 레벨을 저장(upsert)하고 즉시 배포된 아티팩트 전체에 적용한다. */
    @Transactional
    public TenantLogLevelResponse setTenantLogLevel(Long tenantId, LogLevel logLevel) {
        Tenant tenant = findTenant(tenantId);
        TenantLogLevelSetting setting = settingRepository.findByTenantId(tenantId)
                .map(existing -> {
                    existing.update(logLevel);
                    return existing;
                })
                .orElseGet(() -> TenantLogLevelSetting.builder().tenant(tenant).logLevel(logLevel).build());
        setting = settingRepository.save(setting);

        applyLogLevelToAllArtifacts(tenant, logLevel);
        return TenantLogLevelResponse.from(setting);
    }

    @Transactional(readOnly = true)
    public TenantLogLevelResponse getTenantLogLevel(Long tenantId) {
        return settingRepository.findByTenantId(tenantId)
                .map(TenantLogLevelResponse::from)
                .orElseThrow(() -> new NoSuchElementException("설정된 로그 레벨이 없습니다: " + tenantId));
    }

    /** 저장된 desired 로그 레벨을 해당 테넌트의 배포된 아티팩트 전체에 재적용한다(드리프트 교정). */
    @Transactional(readOnly = true)
    public void reapplyTenantLogLevel(Long tenantId) {
        TenantLogLevelSetting setting = settingRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NoSuchElementException("설정된 로그 레벨이 없습니다: " + tenantId));
        applyLogLevelToAllArtifacts(setting.getTenant(), setting.getLogLevel());
    }

    /** 저장된 로그 레벨 설정이 있는 테넌트 ID 목록 (스케줄러가 순회할 대상). */
    @Transactional(readOnly = true)
    public List<Long> listTenantIdsWithSetting() {
        return settingRepository.findAll().stream()
                .map(setting -> setting.getTenant().getId())
                .toList();
    }

    private void applyLogLevelToAllArtifacts(Tenant tenant, LogLevel logLevel) {
        List<SapRuntimeArtifactDto> targets = odataClient.getRuntimeArtifacts(tenant).stream()
                .filter(artifact -> STARTED_STATUS.equals(artifact.Status()))
                .toList();

        Semaphore concurrencyLimiter = new Semaphore(MAX_CONCURRENT_SAP_CALLS);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SapRuntimeArtifactDto artifact : targets) {
                executor.submit(() -> {
                    try {
                        concurrencyLimiter.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    try {
                        odataClient.setMplLogLevel(tenant, artifact.Id(), logLevel.name());
                    } catch (Exception e) {
                        log.error("아티팩트 로그 레벨 설정 실패 - Tenant: {}, Artifact: {}, Level: {}. Error: {}",
                                tenant.getId(), artifact.Id(), logLevel, e.getMessage());
                    } finally {
                        concurrencyLimiter.release();
                    }
                });
            }
        }
    }

    private Tenant findTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + tenantId));
    }
}
