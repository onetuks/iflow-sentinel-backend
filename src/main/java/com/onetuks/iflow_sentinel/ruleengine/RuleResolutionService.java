package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.domain.binding.Binding;
import com.onetuks.iflow_sentinel.domain.binding.BindingOverride;
import com.onetuks.iflow_sentinel.domain.binding.BindingOverrideRepository;
import com.onetuks.iflow_sentinel.domain.binding.BindingRepository;
import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.Severity;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Binding에 적용할 유효 규칙 목록을 계산한다: ruleset과 그 imports를 재귀 평탄화(순환 참조 가드 포함)해
 * 모은 뒤, 같은 Binding의 BindingOverride(ruleId 기준)로 severity/enabled를 재정의한다.
 * bindingId를 받아 이 메서드 안에서 직접 조회한다 — 호출자가 이미 들고 있는(다른 세션에서 로딩된) detached
 * Binding을 넘기면 ruleset/rules/imports 지연 로딩 시 LazyInitializationException이 나므로,
 * 반드시 이 트랜잭션 경계 안에서 새로 조회해야 한다.
 */
@Service
public class RuleResolutionService {

    private final BindingRepository bindingRepository;
    private final BindingOverrideRepository bindingOverrideRepository;

    public RuleResolutionService(BindingRepository bindingRepository, BindingOverrideRepository bindingOverrideRepository) {
        this.bindingRepository = bindingRepository;
        this.bindingOverrideRepository = bindingOverrideRepository;
    }

    @Transactional
    public List<EffectiveRule> resolveEffectiveRules(Long bindingId) {
        Binding binding = bindingRepository.findById(bindingId)
                .orElseThrow(() -> new NoSuchElementException("바인딩을 찾을 수 없습니다: " + bindingId));
        List<Rule> rules = flattenRules(binding.getRuleset(), new HashSet<>());

        Map<Long, BindingOverride> overridesByRuleId = new HashMap<>();
        for (BindingOverride override : bindingOverrideRepository.findByBindingId(binding.getId())) {
            overridesByRuleId.put(override.getRule().getId(), override);
        }

        Map<Long, EffectiveRule> effectiveById = new LinkedHashMap<>();
        for (Rule rule : rules) {
            if (effectiveById.containsKey(rule.getId())) {
                continue;
            }
            BindingOverride override = overridesByRuleId.get(rule.getId());
            Severity severity = (override != null && override.getOverriddenSeverity() != null)
                    ? override.getOverriddenSeverity() : rule.getSeverity();
            boolean enabled = (override != null && override.getOverriddenEnabled() != null)
                    ? override.getOverriddenEnabled() : rule.isEnabled();
            effectiveById.put(rule.getId(), new EffectiveRule(rule, severity, enabled));
        }
        return new ArrayList<>(effectiveById.values());
    }

    private List<Rule> flattenRules(Ruleset ruleset, Set<Long> visited) {
        if (!visited.add(ruleset.getId())) {
            return List.of();
        }
        List<Rule> result = new ArrayList<>(ruleset.getRules());
        for (Ruleset imported : ruleset.getImports()) {
            result.addAll(flattenRules(imported, visited));
        }
        return result;
    }
}
