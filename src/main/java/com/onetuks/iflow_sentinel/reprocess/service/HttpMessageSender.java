package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.reprocess.domain.ProtocolType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * HTTP(REST) 인터페이스로 재처리 페이로드를 전송한다. 기존 재처리 로직과 동일하게
 * payload 형태(JSON/XML/plain)를 추측해 Content-Type을 결정한다.
 */
@Component
public class HttpMessageSender implements MessageSender {

    private final SapODataClient sapODataClient;

    public HttpMessageSender(SapODataClient sapODataClient) {
        this.sapODataClient = sapODataClient;
    }

    @Override
    public boolean supports(ProtocolType protocolType) {
        return protocolType == ProtocolType.HTTP;
    }

    @Override
    public ResponseEntity<String> send(Tenant tenant, String targetUrl, String payload, String soapAction) {
        return sapODataClient.callInterfaceEndpoint(tenant, targetUrl, payload, determineContentType(payload));
    }

    private String determineContentType(String payload) {
        if (payload == null || payload.isBlank()) {
            return "application/json";
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "application/json";
        } else if (trimmed.startsWith("<")) {
            return "application/xml";
        }
        return "text/plain;charset=UTF-8";
    }
}
