package com.onetuks.iflow_sentinel.domain.rule;

import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
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
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruleset_id")
    private Ruleset ruleset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> target;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> params;

    private String message;

    @Column(nullable = false)
    private boolean enabled;

    @Builder
    public Rule(
            String ruleKey,
            RuleType type,
            Severity severity,
            Map<String, Object> target,
            Map<String, Object> params,
            String message,
            boolean enabled) {
        this.ruleKey = ruleKey;
        this.type = type;
        this.severity = severity;
        this.target = target;
        this.params = params;
        this.message = message;
        this.enabled = enabled;
    }

    public void assignRuleset(Ruleset ruleset) {
        this.ruleset = ruleset;
    }
}
