package com.onetuks.iflow_sentinel.connector.domain.integrationpackage;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntegrationPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false, unique = true)
    private String sapPackageId;

    @Column(nullable = false)
    private String name;

    /** SAP IntegrationPackages의 Mode 원본값 (예: EDIT_ALLOWED, READ_ONLY). */
    @Column
    private String mode;

    @Builder
    public IntegrationPackage(Tenant tenant, String sapPackageId, String name, String mode) {
        this.tenant = tenant;
        this.sapPackageId = sapPackageId;
        this.name = name;
        this.mode = mode;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void updateMode(String mode) {
        this.mode = mode;
    }

    /** 아티팩트 동기화 대상 여부 - SAP에서 편집 가능(EDIT_ALLOWED)한 패키지만 대상으로 한다. */
    public boolean isEditable() {
        return "EDIT_ALLOWED".equals(mode);
    }
}
