package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.dto.ArtifactResponse;
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
}
