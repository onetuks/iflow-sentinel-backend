package com.onetuks.iflow_sentinel.reprocess.domain;

/**
 * 저장소 매핑 정보 신뢰도 (3단계 구조).
 * AUTO_PARSED: 1단계 - XML 파싱 기반 자동 추출
 * ESTIMATED: 2단계 - SAP API/동적 파라미터 기반 후보 추정
 * MANUAL: 3단계 - 사용자 수동 입력/오버라이드
 */
public enum ConfidenceLevel {
    AUTO_PARSED,
    ESTIMATED,
    MANUAL
}
