package com.onetuks.iflow_sentinel.reprocess.domain;

/**
 * 인터페이스 직접 호출 재처리 시 사용할 통신 프로토콜.
 * 지정되지 않으면(null) {@link com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService}에서
 * 기존 동작과의 하위 호환을 위해 HTTP로 취급한다.
 */
public enum ProtocolType {
    HTTP,
    SOAP
}
