package com.onetuks.iflow_sentinel.rule;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.rule.domain.Rule;
import com.onetuks.iflow_sentinel.rule.domain.RuleRepository;
import com.onetuks.iflow_sentinel.rule.domain.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.Severity;
import com.onetuks.iflow_sentinel.rule.dto.RuleCreateRequest;
import com.onetuks.iflow_sentinel.rule.dto.RuleResponse;
import com.onetuks.iflow_sentinel.rule.dto.RuleUpdateRequest;
import com.onetuks.iflow_sentinel.rule.service.RuleService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RUL-002~011: 규칙 CRUD와 isGlobal/customProject 조합 검증(설계서 6.4 — 전역/프로젝트 규칙은
 * 상속·오버라이드 없이 독립적이어야 하므로, 둘 중 하나만 성립해야 한다).
 */
@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Test
    void createsGlobalRuleWithoutCustomProject() {
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleService service = new RuleService(ruleRepository, projectRepository);
        RuleResponse response = service.create(new RuleCreateRequest(
                "sender-naming", true, null, RuleType.NAMING_CONVENTION, Severity.FAIL,
                Map.of(), Map.of("prefix", "OP_"), "메시지", true));

        assertThat(response.isGlobal()).isTrue();
        assertThat(response.customProjectId()).isNull();
    }

    @Test
    void createsProjectRuleWithCustomProject() {
        Project project = Project.builder().name("Test Project").build();
        ReflectionTestUtils.setField(project, "id", 5L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleService service = new RuleService(ruleRepository, projectRepository);
        RuleResponse response = service.create(new RuleCreateRequest(
                "mapping-step-limit", false, 5L, RuleType.MAPPING_TYPE, Severity.WARN,
                Map.of(), Map.of(), "메시지", true));

        assertThat(response.isGlobal()).isFalse();
        assertThat(response.customProjectId()).isEqualTo(5L);
    }

    @Test
    void globalRuleWithCustomProjectIdIsRejected() {
        RuleService service = new RuleService(ruleRepository, projectRepository);

        assertThatThrownBy(() -> service.create(new RuleCreateRequest(
                "bad-rule", true, 5L, RuleType.NAMING_CONVENTION, Severity.FAIL,
                Map.of(), Map.of(), "메시지", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void projectRuleWithoutCustomProjectIdIsRejected() {
        RuleService service = new RuleService(ruleRepository, projectRepository);

        assertThatThrownBy(() -> service.create(new RuleCreateRequest(
                "bad-rule", false, null, RuleType.NAMING_CONVENTION, Severity.FAIL,
                Map.of(), Map.of(), "메시지", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateActuallyAppliesNewValuesUnlikeThePriorStub() {
        Rule rule = Rule.builder()
                .ruleKey("sender-naming")
                .isGlobal(true)
                .type(RuleType.NAMING_CONVENTION)
                .severity(Severity.WARN)
                .target(Map.of())
                .params(Map.of("prefix", "OLD_"))
                .message("old message")
                .enabled(true)
                .build();
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleService service = new RuleService(ruleRepository, projectRepository);
        RuleResponse response = service.update(1L, new RuleUpdateRequest(
                Severity.FAIL, Map.of(), Map.of("prefix", "NEW_"), "new message", false));

        assertThat(response.severity()).isEqualTo("FAIL");
        assertThat(response.params()).containsEntry("prefix", "NEW_");
        assertThat(response.message()).isEqualTo("new message");
        assertThat(response.enabled()).isFalse();
    }
}
