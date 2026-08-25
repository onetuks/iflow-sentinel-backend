package com.onetuks.iflow_sentinel.exception;

/**
 * SAP IS 테넌트와의 통신(OAuth2 토큰 발급, OData 호출) 중 발생한 오류. statusCode는 HTTP 응답이 없으면
 * -1.
 */
public class ConnectorException extends RuntimeException {

    private final int statusCode;

    public ConnectorException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ConnectorException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
