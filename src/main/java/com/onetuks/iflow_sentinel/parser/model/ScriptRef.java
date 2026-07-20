package com.onetuks.iflow_sentinel.parser.model;

/** activityType=Script 스텝의 정규화 필드. language는 file 확장자로부터 Parser가 파생. */
public record ScriptRef(String file, String language, String bundleId, String function) {
}
