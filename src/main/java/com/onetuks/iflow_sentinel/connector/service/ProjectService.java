package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.dto.ProjectRequest;
import com.onetuks.iflow_sentinel.connector.dto.ProjectResponse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = Project.builder().name(request.name()).build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }
}
