package com.onetuks.iflow_sentinel.domain.binding;

import com.onetuks.iflow_sentinel.domain.project.Project;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import jakarta.persistence.CascadeType;
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
public class Binding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruleset_id", nullable = false)
    private Ruleset ruleset;

    @OneToMany(mappedBy = "binding", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BindingOverride> overrides = new ArrayList<>();

    @Builder
    public Binding(Project project, Ruleset ruleset) {
        this.project = project;
        this.ruleset = ruleset;
    }

    public void addOverride(BindingOverride override) {
        overrides.add(override);
        override.assignBinding(this);
    }
}
