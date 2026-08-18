package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** SAP IS OData V2 단일 엔티티 응답 래퍼: {"d":{...}}. 컬렉션 응답({"d":{"results":[...]}})과 구조가 다르다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ODataEntityResponse<T>(T d) {
}
