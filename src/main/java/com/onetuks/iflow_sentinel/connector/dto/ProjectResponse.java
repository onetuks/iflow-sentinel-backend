package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.domain.project.Project;

public record ProjectResponse(Long id, String name) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName());
    }
}
