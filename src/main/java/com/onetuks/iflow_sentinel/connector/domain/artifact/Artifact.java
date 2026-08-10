package com.onetuks.iflow_sentinel.connector.domain.artifact;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;

import jakarta.persistence.Column;
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
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private IntegrationPackage integrationPackage;

    @Column(nullable = false)
    private String sapArtifactId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtifactType type;

    @Builder
    public Artifact(IntegrationPackage integrationPackage, String sapArtifactId, String name, String version,
            ArtifactType type) {
        this.integrationPackage = integrationPackage;
        this.sapArtifactId = sapArtifactId;
        this.name = name;
        this.version = version;
        this.type = type;
    }

    public void updateFrom(IntegrationPackage integrationPackage, String name, String version, ArtifactType type) {
        this.integrationPackage = integrationPackage;
        this.name = name;
        this.version = version;
        this.type = type;
    }
}
