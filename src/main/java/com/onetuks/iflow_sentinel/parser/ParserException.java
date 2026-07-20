package com.onetuks.iflow_sentinel.parser;

/** 아티팩트 ZIP이 손상되었거나, 필수 리소스가 없거나, 핵심 XML이 malformed일 때 던지는 unchecked 예외. */
public class ParserException extends RuntimeException {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
