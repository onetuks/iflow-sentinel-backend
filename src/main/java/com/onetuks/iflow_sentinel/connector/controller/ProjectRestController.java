package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.dto.ProjectCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.ProjectResponse;
import com.onetuks.iflow_sentinel.connector.dto.ProjectResponse.ProjectResponses;
import com.onetuks.iflow_sentinel.connector.dto.ProjectUpdateRequest;
import com.onetuks.iflow_sentinel.connector.service.ProjectService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = "/api/connectors/projects",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProjectRestController {

  private final ProjectService projectService;

  @PostMapping
  public ResponseEntity<String> createProject(@RequestBody @Valid ProjectCreateRequest request) {
    Project result = projectService.createProject(request);
    return ResponseEntity.created(URI.create("/api/connectors/projects/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editProject(@PathVariable Long id, @RequestBody @Valid ProjectUpdateRequest request) {
    projectService.updateProject(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<ProjectResponse> searchProject(@PathVariable Long id) {
    Project result = projectService.getProjectById(id);
    return ResponseEntity.ok(ProjectResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<ProjectResponses> searchProjects(@PageableDefault Pageable pageable) {
    Page<Project> results = projectService.getProjects(pageable);
    return ResponseEntity.ok(ProjectResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
    projectService.removeProject(id);
    return ResponseEntity.noContent().build();
  }
}
