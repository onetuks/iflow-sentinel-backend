package com.onetuks.iflow_sentinel.reprocess.domain;

/**
 * iFlow 아티팩트의 메시지 재처리 지원 유형.
 */
public enum ReprocessSupportType {
    NONE,
    DATASTORE_ONLY,
    JMS_ONLY,
    BOTH
}
