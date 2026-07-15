package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.dto.ProjectCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.ProjectUpdateRequest;
import com.onetuks.iflow_sentinel.connector.persistence.ProjectJpaRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectJpaRepository projectRepository;

  @Transactional
  public Project createProject(ProjectCreateRequest request) {
    Project newProject = Project.builder()
        .name(request.name())
        .build();

    return projectRepository.save(newProject);
  }

  @Transactional
  public Project updateProject(Long id, ProjectUpdateRequest request) {
    Project project = projectRepository.findById(id).orElseThrow(NoSuchElementException::new);
    return project.setName(request.name());
  }

  @Transactional(readOnly = true)
  public Project getProjectById(Long id) {
    return projectRepository.findById(id).orElseThrow(NoSuchElementException::new);
  }

  @Transactional(readOnly = true)
  public Project getProjectByName(String name) {
    return projectRepository.findByName(name).orElseThrow(NoSuchElementException::new);
  }

  @Transactional
  public void removeProject(Long id) {
    projectRepository.deleteById(id);
  }
}
