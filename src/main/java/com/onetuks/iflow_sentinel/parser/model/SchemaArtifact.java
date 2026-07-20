package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/** *.wsdl 파일에서 추출한 인터페이스 스키마. */
public record SchemaArtifact(String file, String name, String targetNamespace, List<String> messageTypes) {
    public SchemaArtifact {
        messageTypes = List.copyOf(messageTypes);
    }
}
