package com.onetuks.iflow_sentinel.connector.domain.tenant;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String odataUrl;

    @Column(nullable = false)
    private String tokenUrl;

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
            Project project,
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret) {
        this.project = project;
        this.name = name;
        this.odataUrl = odataUrl;
        this.tokenUrl = tokenUrl;
        this.platformType = platformType;
        this.authType = authType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
