package com.onetuks.iflow_sentinel.rule.service;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.domain.rule.RuleRepository;
import com.onetuks.iflow_sentinel.rule.dto.RuleCreateRequest;
import com.onetuks.iflow_sentinel.rule.dto.RuleResponse;
import com.onetuks.iflow_sentinel.rule.dto.RuleUpdateRequest;

import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 규칙 라이브러리 CRUD(RUL-002~011). isGlobal=true인 전역 규칙과 isGlobal=false인 프로젝트 전용 규칙은
 * 상속·오버라이드 없이 완전히 독립적이다(설계서 6.4) — 다른 값이 필요하면 새 규칙을 복제해 만든다.
 */
@Service
public class RuleService {

    private final RuleRepository ruleRepository;
    private final ProjectRepository projectRepository;

    public RuleService(RuleRepository ruleRepository, ProjectRepository projectRepository) {
        this.ruleRepository = ruleRepository;
        this.projectRepository = projectRepository;
    }

    public RuleResponse create(RuleCreateRequest request) {
        Project customProject = resolveCustomProject(request.isGlobal(), request.customProjectId());

        Rule rule = Rule.builder()
                .ruleKey(request.ruleKey())
                .isGlobal(request.isGlobal())
                .customProject(customProject)
                .type(request.type())
                .severity(request.severity())
                .target(request.target())
                .params(request.params())
                .message(request.message())
                .enabled(request.enabled())
                .build();
        return RuleResponse.from(ruleRepository.save(rule));
    }

    public RuleResponse get(Long id) {
        return RuleResponse.from(findRule(id));
    }

    public RuleResponse update(Long id, RuleUpdateRequest request) {
        Rule rule = findRule(id);
        rule.updateDefinition(request.severity(), request.target(), request.params(), request.message(), request.enabled());
        return RuleResponse.from(ruleRepository.save(rule));
    }

    public void delete(Long id) {
        ruleRepository.deleteById(id);
    }

    private Project resolveCustomProject(Boolean isGlobal, Long customProjectId) {
        boolean global = Boolean.TRUE.equals(isGlobal);
        if (global) {
            if (customProjectId != null) {
                throw new IllegalArgumentException("전역 규칙(isGlobal=true)에는 customProjectId를 지정할 수 없습니다.");
            }
            return null;
        }
        if (customProjectId == null) {
            throw new IllegalArgumentException("프로젝트 규칙(isGlobal=false)은 customProjectId가 필요합니다.");
        }
        return projectRepository.findById(customProjectId)
                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + customProjectId));
    }

    private Rule findRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("규칙을 찾을 수 없습니다: " + id));
    }
}
