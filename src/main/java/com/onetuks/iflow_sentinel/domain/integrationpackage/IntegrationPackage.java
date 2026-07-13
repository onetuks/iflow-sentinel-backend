package com.onetuks.iflow_sentinel.domain.integrationpackage;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.project.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
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
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, unique = true)
    private String sapPackageId;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "integrationPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Artifact> artifacts = new ArrayList<>();

    @Builder
    public IntegrationPackage(String sapPackageId, String name) {
        this.sapPackageId = sapPackageId;
        this.name = name;
    }

    public void assignProject(Project project) {
        this.project = project;
    }

    public void addArtifact(Artifact artifact) {
        artifacts.add(artifact);
        artifact.assignIntegrationPackage(this);
    }
}
