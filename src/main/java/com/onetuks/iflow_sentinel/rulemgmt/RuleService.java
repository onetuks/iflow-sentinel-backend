package com.onetuks.iflow_sentinel.rulemgmt;

import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.RuleRepository;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import com.onetuks.iflow_sentinel.domain.ruleset.RulesetRepository;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RuleRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RuleResponse;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class RuleService {

    private final RulesetRepository rulesetRepository;
    private final RuleRepository ruleRepository;

    public RuleService(RulesetRepository rulesetRepository, RuleRepository ruleRepository) {
        this.rulesetRepository = rulesetRepository;
        this.ruleRepository = ruleRepository;
    }

    public RuleResponse create(Long rulesetId, RuleRequest request) {
        Ruleset ruleset = rulesetRepository.findById(rulesetId)
                .orElseThrow(() -> new NoSuchElementException("룰셋을 찾을 수 없습니다: " + rulesetId));

        Rule rule = Rule.builder()
                .ruleKey(request.ruleKey())
                .type(request.type())
                .severity(request.severity())
                .target(request.target())
                .params(request.params())
                .message(request.message())
                .enabled(request.enabled())
                .build();
        ruleset.addRule(rule);
        rulesetRepository.save(ruleset);
        return RuleResponse.from(rule);
    }

    public RuleResponse get(Long id) {
        return RuleResponse.from(findRule(id));
    }

    public RuleResponse update(Long id, RuleRequest request) {
        Rule rule = findRule(id);
        rule.updateDefinition(request.severity(), request.target(), request.params(), request.message(), request.enabled());
        return RuleResponse.from(ruleRepository.save(rule));
    }

    public void delete(Long id) {
        ruleRepository.deleteById(id);
    }

    private Rule findRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("규칙을 찾을 수 없습니다: " + id));
    }
}
