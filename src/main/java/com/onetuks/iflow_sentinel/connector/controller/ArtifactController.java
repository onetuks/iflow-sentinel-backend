package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.dto.ArtifactResponse;
import com.onetuks.iflow_sentinel.connector.service.ArtifactService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/packages/{packageId}/artifacts")
public class ArtifactController {

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @PostMapping("/sync")
    public List<ArtifactResponse> sync(@PathVariable Long packageId) {
        return artifactService.sync(packageId);
    }

    @GetMapping
    public List<ArtifactResponse> list(@PathVariable Long packageId) {
        return artifactService.list(packageId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long packageId, @PathVariable String id) {
        artifactService.delete(id);
    }
}
