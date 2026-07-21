package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRule;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRuleRepository;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 프로젝트에 적용된(ProjectRule.isEnabled=true) 규칙만 EffectiveRule로 해석하는지 검증한다.
 * Ruleset/Binding 상속·오버라이드가 제거된 이후의 새 해석 방식(설계서 6.4)이 대상이다.
 */
@ExtendWith(MockitoExtension.class)
class RuleResolutionServiceTest {

    @Mock
    private ProjectRuleRepository projectRuleRepository;

    @Test
    void onlyEnabledProjectRulesBecomeEffectiveRules() {
        Project project = Project.builder().name("Test Project").build();
        ReflectionTestUtils.setField(project, "id", 1L);

        Rule enabledRule = rule("enabled-rule");
        ProjectRule enabled = ProjectRule.builder().project(project).rule(enabledRule).isEnabled(true).build();

        when(projectRuleRepository.findByProjectIdAndIsEnabledTrue(1L)).thenReturn(List.of(enabled));

        RuleResolutionService service = new RuleResolutionService(projectRuleRepository);
        List<EffectiveRule> effectiveRules = service.resolveEffectiveRules(1L);

        assertThat(effectiveRules).hasSize(1);
        assertThat(effectiveRules.get(0).rule().getRuleKey()).isEqualTo("enabled-rule");
        assertThat(effectiveRules.get(0).severity()).isEqualTo(Severity.FAIL);
        assertThat(effectiveRules.get(0).enabled()).isTrue();
    }

    private static Rule rule(String ruleKey) {
        return Rule.builder()
                .ruleKey(ruleKey)
                .isGlobal(true)
                .type(RuleType.REQUIRED_ERROR_HANDLER)
                .severity(Severity.FAIL)
                .target(Map.of())
                .params(Map.of())
                .message("test")
                .enabled(true)
                .build();
    }
}
