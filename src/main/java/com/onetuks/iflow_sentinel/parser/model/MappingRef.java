package com.onetuks.iflow_sentinel.parser.model;

/** activityType=Mapping 스텝의 정규화 필드. */
public record MappingRef(String type, String name, String uri, String reference) {
}
