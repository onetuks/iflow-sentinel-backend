package com.onetuks.iflow_sentinel.parser.model;

/** activityType=ProcessCallElement 스텝의 정규화 필드. */
public record CallRef(String processId, String subActivityType) {
}
