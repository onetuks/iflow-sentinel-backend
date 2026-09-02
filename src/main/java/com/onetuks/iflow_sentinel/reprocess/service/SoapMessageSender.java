package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.reprocess.domain.ProtocolType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * SOAP 인터페이스로 재처리 페이로드를 전송한다. HTTP sender와 달리 Content-Type을 추측하지 않고
 * 항상 {@code text/xml}로 고정하며, 호출 대상 인터페이스가 요구하는 {@code SOAPAction} 헤더를 실어 보낸다.
 * payload는 이미 완성된 SOAP Envelope(XML)이어야 한다 — DataStore/MPL에 저장된 실패 메시지 원문이 여기 해당한다.
 */
@Component
public class SoapMessageSender implements MessageSender {

    private final SapODataClient sapODataClient;

    public SoapMessageSender(SapODataClient sapODataClient) {
        this.sapODataClient = sapODataClient;
    }

    @Override
    public boolean supports(ProtocolType protocolType) {
        return protocolType == ProtocolType.SOAP;
    }

    @Override
    public ResponseEntity<String> send(Tenant tenant, String targetUrl, String payload, String soapAction) {
        return sapODataClient.callSoapInterfaceEndpoint(tenant, targetUrl, payload, soapAction);
    }
}
