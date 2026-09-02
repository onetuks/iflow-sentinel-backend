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

import java.time.LocalDate;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlatform platformType;

    /** API(OData) 활용을 위한 자격증명. */
    @Column(name = "api_url", nullable = false)
    private String apiUrl;

    @Column(name = "api_token_url", nullable = false)
    private String apiTokenUrl;

    @Column(name = "api_client_id", nullable = false)
    private String apiClientId;

    @Convert(converter = CredentialConverter.class)
    @Column(name = "api_client_secret", nullable = false)
    private String apiClientSecret;

    @Column(name = "api_create_date")
    private LocalDate apiCreateDate;

    /** 인터페이스(iFlow 엔드포인트) 호출을 위한 자격증명. */
    @Column(name = "if_url")
    private String ifUrl;

    @Column(name = "if_token_url")
    private String ifTokenUrl;

    @Column(name = "if_client_id")
    private String ifClientID;

    @Convert(converter = CredentialConverter.class)
    @Column(name = "if_client_secret")
    private String ifClientSecret;

    @Column(name = "if_create_date")
    private LocalDate ifCreateDate;

    @Builder
    public Tenant(
            Project project,
            String name,
            TenantPlatform platformType,
            String apiUrl,
            String apiTokenUrl,
            String apiClientId,
            String apiClientSecret,
            LocalDate apiCreateDate,
            String ifUrl,
            String ifTokenUrl,
            String ifClientID,
            String ifClientSecret,
            LocalDate ifCreateDate) {
        this.project = project;
        this.name = name;
        this.platformType = platformType;
        this.apiUrl = apiUrl;
        this.apiTokenUrl = apiTokenUrl;
        this.apiClientId = apiClientId;
        this.apiClientSecret = apiClientSecret;
        this.apiCreateDate = apiCreateDate;
        this.ifUrl = ifUrl;
        this.ifTokenUrl = ifTokenUrl;
        this.ifClientID = ifClientID;
        this.ifClientSecret = ifClientSecret;
        this.ifCreateDate = ifCreateDate;
    }

    public void update(
            String name,
            TenantPlatform platformType,
            String apiUrl,
            String apiTokenUrl,
            String apiClientId,
            String apiClientSecret,
            LocalDate apiCreateDate,
            String ifUrl,
            String ifTokenUrl,
            String ifClientID,
            String ifClientSecret,
            LocalDate ifCreateDate) {
        this.name = name;
        this.platformType = platformType;
        this.apiUrl = apiUrl;
        this.apiTokenUrl = apiTokenUrl;
        this.apiClientId = apiClientId;
        this.apiClientSecret = apiClientSecret;
        this.apiCreateDate = apiCreateDate;
        this.ifUrl = ifUrl;
        this.ifTokenUrl = ifTokenUrl;
        this.ifClientID = ifClientID;
        this.ifClientSecret = ifClientSecret;
        this.ifCreateDate = ifCreateDate;
    }
}
