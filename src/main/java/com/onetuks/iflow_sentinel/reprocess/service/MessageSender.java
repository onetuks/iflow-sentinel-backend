package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.reprocess.domain.ProtocolType;
import org.springframework.http.ResponseEntity;

/**
 * 인터페이스 직접 호출 재처리의 실제 전송을 담당하는 전략 인터페이스.
 * 프로토콜(HTTP/SOAP/...)별로 구현체를 두고, {@link MessageReprocessService}는
 * {@link ProtocolType}에 맞는 구현체를 선택해 위임하기만 한다.
 */
public interface MessageSender {

    boolean supports(ProtocolType protocolType);

    ResponseEntity<String> send(Tenant tenant, String targetUrl, String payload, String soapAction);
}
