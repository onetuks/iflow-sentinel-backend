package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRule;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트에 적용할 유효 규칙 목록을 계산한다: 해당 프로젝트의 ProjectRule 중 isEnabled=true인 것만 모아
 * 각 Rule을 그대로(override 없이) EffectiveRule로 변환한다. 다른 값이 필요한 경우는 override가 아니라
 * 규칙을 복제해 별도로 켜는 방식(RUL-011)으로 처리하므로, 여기서는 severity/enabled를 재정의하지 않는다.
 */
@Service
public class RuleResolutionService {

    private final ProjectRuleRepository projectRuleRepository;

    public RuleResolutionService(ProjectRuleRepository projectRuleRepository) {
        this.projectRuleRepository = projectRuleRepository;
    }

    @Transactional
    public List<EffectiveRule> resolveEffectiveRules(Long projectId) {
        List<ProjectRule> projectRules = projectRuleRepository.findByProjectIdAndIsEnabledTrue(projectId);

        List<EffectiveRule> effectiveRules = new ArrayList<>();
        for (ProjectRule projectRule : projectRules) {
            effectiveRules.add(new EffectiveRule(
                    projectRule.getRule(), projectRule.getRule().getSeverity(), projectRule.getRule().isEnabled()));
        }
        return effectiveRules;
    }
}
