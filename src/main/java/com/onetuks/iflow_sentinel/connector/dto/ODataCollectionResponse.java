package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** SAP IS OData V2 컬렉션 응답 래퍼: {"d":{"results":[...]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ODataCollectionResponse<T>(ODataResults<T> d) {
}
