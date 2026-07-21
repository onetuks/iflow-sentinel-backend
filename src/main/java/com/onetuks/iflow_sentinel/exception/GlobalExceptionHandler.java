package com.onetuks.iflow_sentinel.exception;

import com.onetuks.iflow_sentinel.parser.ParserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/** 컨트롤러 전반에서 공통으로 발생하는 예외를 의미 있는 HTTP 상태 코드로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ConnectorException.class)
    public ResponseEntity<ErrorResponse> handleConnector(ConnectorException e) {
        HttpStatus status = (e.statusCode() >= 400 && e.statusCode() < 600)
                ? HttpStatus.valueOf(e.statusCode())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ParserException.class)
    public ResponseEntity<ErrorResponse> handleParser(ParserException e) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(new ErrorResponse(e.getMessage()));
    }
}
