package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRule;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRuleRepository;
import com.onetuks.iflow_sentinel.connector.dto.ProjectRuleResponse;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.domain.rule.RuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/** PJR-003/004/005: 프로젝트가 사용할 전역·프로젝트 규칙을 선택해 켜고 끈다. */
@Service
public class ProjectRuleService {

    private final ProjectRepository projectRepository;
    private final RuleRepository ruleRepository;
    private final ProjectRuleRepository projectRuleRepository;

    public ProjectRuleService(
            ProjectRepository projectRepository,
            RuleRepository ruleRepository,
            ProjectRuleRepository projectRuleRepository) {
        this.projectRepository = projectRepository;
        this.ruleRepository = ruleRepository;
        this.projectRuleRepository = projectRuleRepository;
    }

    public List<ProjectRuleResponse> listApplicableRules(Long projectId) {
        findProject(projectId);
        List<Rule> applicableRules = ruleRepository.findByIsGlobalTrueOrCustomProjectId(projectId);
        List<ProjectRule> projectRules = projectRuleRepository.findByProjectId(projectId);

        return applicableRules.stream()
                .map(rule -> {
                    boolean isEnabled = projectRules.stream()
                            .filter(pr -> pr.getRule().getId().equals(rule.getId()))
                            .findFirst()
                            .map(pr -> Boolean.TRUE.equals(pr.getIsEnabled()))
                            .orElse(false);
                    return ProjectRuleResponse.of(rule, isEnabled);
                })
                .toList();
    }

    public ProjectRuleResponse setEnabled(Long projectId, Long ruleId, Boolean isEnabled) {
        Project project = findProject(projectId);
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("규칙을 찾을 수 없습니다: " + ruleId));

        ProjectRule projectRule = projectRuleRepository.findByProjectIdAndRuleId(projectId, ruleId)
                .orElseGet(() -> ProjectRule.builder().project(project).rule(rule).isEnabled(isEnabled).build());
        projectRule.updateEnabled(isEnabled);
        projectRuleRepository.save(projectRule);

        return ProjectRuleResponse.of(rule, Boolean.TRUE.equals(isEnabled));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + projectId));
    }
}
