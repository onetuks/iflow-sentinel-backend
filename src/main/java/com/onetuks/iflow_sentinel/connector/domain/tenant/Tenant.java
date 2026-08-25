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

    @Column(name = "interface_url")
    private String interfaceUrl;

    @Column(name = "interface_token_url")
    private String interfaceTokenUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "interface_auth_type")
    private TenantAuthType interfaceAuthType;

    @Column(name = "interface_username")
    private String interfaceUsername;

    @Convert(converter = CredentialConverter.class)
    @Column(name = "interface_password")
    private String interfacePassword;

    @Builder
    public Tenant(
            Project project,
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret,
            String interfaceUrl,
            String interfaceTokenUrl,
            TenantAuthType interfaceAuthType,
            String interfaceUsername,
            String interfacePassword) {
        this.project = project;
        this.name = name;
        this.odataUrl = odataUrl;
        this.tokenUrl = tokenUrl;
        this.platformType = platformType;
        this.authType = authType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.interfaceUrl = interfaceUrl;
        this.interfaceTokenUrl = interfaceTokenUrl;
        this.interfaceAuthType = interfaceAuthType != null ? interfaceAuthType : TenantAuthType.BASIC;
        this.interfaceUsername = interfaceUsername;
        this.interfacePassword = interfacePassword;
    }

    public void update(
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret,
            String interfaceUrl,
            String interfaceTokenUrl,
            TenantAuthType interfaceAuthType,
            String interfaceUsername,
            String interfacePassword) {
        this.name = name;
        this.odataUrl = odataUrl;
        this.tokenUrl = tokenUrl;
        this.platformType = platformType;
        this.authType = authType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.interfaceUrl = interfaceUrl;
        this.interfaceTokenUrl = interfaceTokenUrl;
        this.interfaceAuthType = interfaceAuthType != null ? interfaceAuthType : TenantAuthType.BASIC;
        this.interfaceUsername = interfaceUsername;
        this.interfacePassword = interfacePassword;
    }
}
