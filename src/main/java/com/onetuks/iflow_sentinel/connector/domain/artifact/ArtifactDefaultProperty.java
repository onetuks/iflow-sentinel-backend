package com.onetuks.iflow_sentinel.connector.domain.artifact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "artifact_default_property",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_artifact_default_prop",
            columnNames = {"sap_artifact_id", "version", "parameter_key"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtifactDefaultProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sap_artifact_id", nullable = false)
    private String sapArtifactId;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "parameter_key", nullable = false)
    private String parameterKey;

    @Column(name = "default_value", nullable = false)
    private String defaultValue;

    @Builder
    public ArtifactDefaultProperty(String sapArtifactId, String version, String parameterKey, String defaultValue) {
        this.sapArtifactId = sapArtifactId;
        this.version = version;
        this.parameterKey = parameterKey;
        this.defaultValue = defaultValue;
    }

    public void updateDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
