package com.onetuks.iflow_sentinel.reprocess.domain;

/**
 * 인터페이스 직접 호출 재처리 시 사용할 통신 프로토콜.
 * 지정되지 않으면(null) {@link com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService}에서
 * 기존 동작과의 하위 호환을 위해 HTTP로 취급한다.
 */
public enum ProtocolType {
    HTTP,
    SOAP,
    /**
     * SAP CPI 내부 전용 어댑터. 같은 테넌트 내 다른 iFlow에서만 호출 가능하며 외부 네트워크로 노출되지
     * 않으므로, 이 프로토콜로 판별된 인터페이스는 직접 재호출을 시도하지 않고 안내 메시지만 반환한다.
     */
    PROCESS_DIRECT
}
