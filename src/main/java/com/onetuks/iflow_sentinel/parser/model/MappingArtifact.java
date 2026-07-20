package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/** *.mmap 파일에서 추출한 메시지 매핑 정의. */
public record MappingArtifact(
        String name,
        String path,
        String uri,
        MessageRef sourceMessage,
        MessageRef targetMessage,
        List<FunctionLibraryRef> functionLibraries,
        List<MappingFunction> functions,
        FieldCount fieldCount
) {
    public MappingArtifact {
        functionLibraries = List.copyOf(functionLibraries);
        functions = List.copyOf(functions);
    }
}
