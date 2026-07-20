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

    @Builder
    public IntegrationPackage(Tenant tenant, String sapPackageId, String name) {
        this.tenant = tenant;
        this.sapPackageId = sapPackageId;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
