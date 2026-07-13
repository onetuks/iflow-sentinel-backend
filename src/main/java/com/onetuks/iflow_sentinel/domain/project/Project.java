package com.onetuks.iflow_sentinel.domain.project;

import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntegrationPackage> packages = new ArrayList<>();

    @Builder
    public Project(String name) {
        this.name = name;
    }

    public void addPackage(IntegrationPackage integrationPackage) {
        packages.add(integrationPackage);
        integrationPackage.assignProject(this);
    }
}
