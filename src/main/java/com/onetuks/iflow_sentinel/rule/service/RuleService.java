package com.onetuks.iflow_sentinel.rule.service;

import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.rule.dto.RuleCreateRequest;
import com.onetuks.iflow_sentinel.rule.dto.RuleUpdateRequest;
import com.onetuks.iflow_sentinel.rule.persistence.RuleJpaRepository;
import com.onetuks.iflow_sentinel.connector.persistence.ProjectJpaRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleJpaRepository ruleRepository;
    private final ProjectJpaRepository projectRepository;

    @Transactional
    public Rule createRule(RuleCreateRequest request) {
        Project project = null;
        if (Boolean.FALSE.equals(request.isGlobal()) && request.customProjectId() != null) {
            project = projectRepository.findById(request.customProjectId())
                    .orElseThrow(NoSuchElementException::new);
        }

        Rule newRule = Rule.builder()
                .ruleKey(request.ruleKey())
                .isGlobal(request.isGlobal())
                .customProject(project)
                .type(request.type())
                .severity(request.severity())
                .target(request.target())
                .params(request.params())
                .message(request.message())
                .enabled(request.enabled())
                .build();

        return ruleRepository.save(newRule);
    }

    @Transactional
    public Rule updateRule(Long id, RuleUpdateRequest request) {
        Rule rule = ruleRepository.findById(id).orElseThrow(NoSuchElementException::new);
        // Add update logic here if Entity supports it
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public Rule getRuleById(Long id) {
        return ruleRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional
    public void removeRule(Long id) {
        ruleRepository.deleteById(id);
    }
}
