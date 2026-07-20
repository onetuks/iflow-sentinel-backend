package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ODataResults<T>(List<T> results) {
}
