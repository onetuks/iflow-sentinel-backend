package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.dto.ProjectRequest;
import com.onetuks.iflow_sentinel.connector.dto.ProjectResponse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

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

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + id));
        project.rename(request.name());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public void delete(Long id) {
        projectRepository.deleteById(id);
    }
}
