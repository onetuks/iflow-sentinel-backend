package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRule;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRuleRepository;
import com.onetuks.iflow_sentinel.connector.dto.ProjectRuleResponse;
import com.onetuks.iflow_sentinel.connector.service.ProjectRuleService;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.domain.rule.RuleRepository;
import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** PJR-003/004/005: 프로젝트에 적용 가능한 규칙 현황 조회 및 켜고 끄기(ProjectRule 토글). */
@ExtendWith(MockitoExtension.class)
class ProjectRuleServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private ProjectRuleRepository projectRuleRepository;

    private final Project project = project(1L);
    private final Rule rule = rule(10L, "sender-naming");

    @Test
    void ruleWithoutProjectRuleRowIsReportedAsDisabled() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ruleRepository.findByIsGlobalTrueOrCustomProjectId(1L)).thenReturn(List.of(rule));
        when(projectRuleRepository.findByProjectId(1L)).thenReturn(List.of());

        ProjectRuleService service = new ProjectRuleService(projectRepository, ruleRepository, projectRuleRepository);
        List<ProjectRuleResponse> responses = service.listApplicableRules(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).ruleId()).isEqualTo(10L);
        assertThat(responses.get(0).isEnabled()).isFalse();
    }

    @Test
    void setEnabledCreatesProjectRuleOnFirstToggle() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(rule));
        when(projectRuleRepository.findByProjectIdAndRuleId(1L, 10L)).thenReturn(Optional.empty());
        when(projectRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRuleService service = new ProjectRuleService(projectRepository, ruleRepository, projectRuleRepository);
        ProjectRuleResponse response = service.setEnabled(1L, 10L, true);

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.ruleId()).isEqualTo(10L);
    }

    @Test
    void setEnabledUpdatesExistingProjectRule() {
        ProjectRule existing = ProjectRule.builder().project(project).rule(rule).isEnabled(true).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(rule));
        when(projectRuleRepository.findByProjectIdAndRuleId(1L, 10L)).thenReturn(Optional.of(existing));
        when(projectRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRuleService service = new ProjectRuleService(projectRepository, ruleRepository, projectRuleRepository);
        ProjectRuleResponse response = service.setEnabled(1L, 10L, false);

        assertThat(response.isEnabled()).isFalse();
        assertThat(existing.getIsEnabled()).isFalse();
    }

    private static Project project(Long id) {
        Project project = Project.builder().name("Test Project").build();
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private static Rule rule(Long id, String ruleKey) {
        Rule rule = Rule.builder()
                .ruleKey(ruleKey)
                .isGlobal(true)
                .type(RuleType.NAMING_CONVENTION)
                .severity(Severity.FAIL)
                .target(Map.of())
                .params(Map.of())
                .message("test")
                .enabled(true)
                .build();
        ReflectionTestUtils.setField(rule, "id", id);
        return rule;
    }
}
