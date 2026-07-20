package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/**
 * Parser가 아티팩트에서 추출한 구조화된 사실의 집합(설계서 5장).
 * 모든 규칙과 사용자 정의 조건식이 참조하는 계약이며, Parser는 이 트리를 조립만 할 뿐 어떤 판단도 내리지 않는다.
 */
public record ParsedModel(
        int schemaVersion,
        ArtifactInfo artifact,
        IflowModel iflow,
        List<Parameter> parameters,
        List<MappingArtifact> mappings,
        List<SchemaArtifact> schemas,
        List<ScriptArtifact> scripts
) {
    public ParsedModel {
        parameters = List.copyOf(parameters);
        mappings = List.copyOf(mappings);
        schemas = List.copyOf(schemas);
        scripts = List.copyOf(scripts);
    }
}
