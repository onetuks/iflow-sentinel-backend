package com.onetuks.iflow_sentinel.connector.controller;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactResponse;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactResponse.ArtifactResponses;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactUpdateRequest;
import com.onetuks.iflow_sentinel.connector.service.ArtifactService;
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
    path = "/api/connectors/artifacts",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ArtifactRestController {

  private final ArtifactService artifactService;

  @PostMapping
  public ResponseEntity<String> createArtifact(@RequestBody @Valid ArtifactCreateRequest request) {
    Artifact result = artifactService.createArtifact(request);
    return ResponseEntity.created(URI.create("/api/connectors/artifacts/" + result.getId())).build();
  }

  @PatchMapping(path = "/{id}")
  public ResponseEntity<Void> editArtifact(@PathVariable Long id, @RequestBody @Valid ArtifactUpdateRequest request) {
    artifactService.updateArtifact(id, request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<ArtifactResponse> searchArtifact(@PathVariable Long id) {
    Artifact result = artifactService.getArtifactById(id);
    return ResponseEntity.ok(ArtifactResponse.from(result));
  }

  @GetMapping
  public ResponseEntity<ArtifactResponses> searchArtifacts(@PageableDefault Pageable pageable) {
    Page<Artifact> results = artifactService.getArtifacts(pageable);
    return ResponseEntity.ok(ArtifactResponses.from(results));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> deleteArtifact(@PathVariable Long id) {
    artifactService.removeArtifact(id);
    return ResponseEntity.noContent().build();
  }
}
