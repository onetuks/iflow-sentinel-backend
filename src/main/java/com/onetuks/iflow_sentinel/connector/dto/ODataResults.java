package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** {@code __next}는 OData V2 JSON 응답의 다음 페이지 절대 URL이다 (마지막 페이지면 null). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ODataResults<T>(List<T> results, @JsonProperty("__next") String next) {
}
