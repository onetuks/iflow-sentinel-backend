package com.onetuks.iflow_sentinel.connector.dto;

/** 테넌트 연결 테스트 결과. statusCode는 HTTP 응답이 없으면(네트워크 오류 등) -1. */
public record ConnectionTestResult(boolean success, int statusCode, String message) {
}
