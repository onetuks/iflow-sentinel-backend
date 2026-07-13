package com.onetuks.iflow_sentinel.domain.ruleset;

import com.onetuks.iflow_sentinel.domain.rule.Rule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
public class Ruleset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rulesetKey;

    @Column(nullable = false)
    private String version;

    private String description;

    @OneToMany(mappedBy = "ruleset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rule> rules = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ruleset_import",
            joinColumns = @JoinColumn(name = "ruleset_id"),
            inverseJoinColumns = @JoinColumn(name = "imported_ruleset_id"))
    private List<Ruleset> imports = new ArrayList<>();

    @Builder
    public Ruleset(String rulesetKey, String version, String description) {
        this.rulesetKey = rulesetKey;
        this.version = version;
        this.description = description;
    }

    public void addRule(Rule rule) {
        rules.add(rule);
        rule.assignRuleset(this);
    }

    public void addImport(Ruleset imported) {
        imports.add(imported);
    }
}
