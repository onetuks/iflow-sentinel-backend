package com.onetuks.iflow_sentinel.ruleengine;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;

/** DB의 Artifact 엔티티와 Parser가 산출한 ParsedModel(사실 집합)을 하나로 묶는다. Finding 저장 시 artifact FK가 필요하다. */
public record ArtifactParsedModel(Artifact artifact, ParsedModel parsedModel) {
}
