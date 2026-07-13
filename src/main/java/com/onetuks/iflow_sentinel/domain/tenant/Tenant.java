package com.onetuks.iflow_sentinel.domain.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String odataUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlatform platformType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantAuthType authType;

    @Column(nullable = false)
    private String clientId;

    @Convert(converter = CredentialConverter.class)
    @Column(nullable = false)
    private String clientSecret;

    @Builder
    public Tenant(
            String name,
            String odataUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret) {
        this.name = name;
        this.odataUrl = odataUrl;
        this.platformType = platformType;
        this.authType = authType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
