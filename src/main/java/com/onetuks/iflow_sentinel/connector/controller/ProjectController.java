package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.dto.ProjectRequest;
import com.onetuks.iflow_sentinel.connector.dto.ProjectResponse;
import com.onetuks.iflow_sentinel.connector.service.ProjectService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectResponse create(@RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.list();
    }
}
