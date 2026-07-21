package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.dto.ProjectRuleResponse;
import com.onetuks.iflow_sentinel.connector.dto.ProjectRuleUpdateRequest;
import com.onetuks.iflow_sentinel.connector.service.ProjectRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/rules")
public class ProjectRuleController {

    private final ProjectRuleService projectRuleService;

    public ProjectRuleController(ProjectRuleService projectRuleService) {
        this.projectRuleService = projectRuleService;
    }

    @GetMapping
    public List<ProjectRuleResponse> list(@PathVariable Long projectId) {
        return projectRuleService.listApplicableRules(projectId);
    }

    @PutMapping("/{ruleId}")
    public ProjectRuleResponse setEnabled(
            @PathVariable Long projectId,
            @PathVariable Long ruleId,
            @RequestBody ProjectRuleUpdateRequest request) {
        return projectRuleService.setEnabled(projectId, ruleId, request.isEnabled());
    }
}
