package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;
import com.onetuks.iflow_sentinel.connector.service.ArtifactExportService;
import com.onetuks.iflow_sentinel.connector.service.ArtifactTrackerActionService;
import com.onetuks.iflow_sentinel.connector.service.ArtifactTrackerService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/tracker-artifacts")
public class ArtifactTrackerController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ArtifactTrackerService artifactTrackerService;
    private final ArtifactTrackerActionService artifactTrackerActionService;
    private final ArtifactExportService artifactExportService;

    public ArtifactTrackerController(ArtifactTrackerService artifactTrackerService,
            ArtifactTrackerActionService artifactTrackerActionService, ArtifactExportService artifactExportService) {
        this.artifactTrackerService = artifactTrackerService;
        this.artifactTrackerActionService = artifactTrackerActionService;
        this.artifactExportService = artifactExportService;
    }

    @GetMapping
    public List<TrackerArtifactResponse> list(@PathVariable Long tenantId) {
        return artifactTrackerService.list(tenantId);
    }

    @PostMapping("/{artifactId}/deploy")
    public void deploy(@PathVariable Long tenantId, @PathVariable String artifactId) {
        artifactTrackerActionService.deploy(tenantId, artifactId);
    }

    @PostMapping("/{artifactId}/undeploy")
    public void undeploy(@PathVariable Long tenantId, @PathVariable String artifactId) {
        artifactTrackerActionService.undeploy(tenantId, artifactId);
    }

    @DeleteMapping("/{artifactId}")
    public void delete(@PathVariable Long tenantId, @PathVariable String artifactId,
            @RequestParam String version) {
        artifactTrackerActionService.deleteDesigntimeArtifact(tenantId, artifactId, version);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@PathVariable Long tenantId,
            @RequestParam(required = false) List<String> artifactIds) {
        byte[] content = artifactExportService.export(tenantId, artifactIds);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"artifacts.xlsx\"")
                .body(content);
    }
}
