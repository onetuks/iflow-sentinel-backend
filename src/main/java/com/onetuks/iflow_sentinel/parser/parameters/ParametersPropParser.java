package com.onetuks.iflow_sentinel.parser.parameters;

import com.onetuks.iflow_sentinel.parser.ParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * parameters.prop(설정값)을 파싱한다. 파라미터명에 공백이 포함되거나(예: "DataStore Sender_Retry_Interval")
 * 값에 콜론이 이스케이프(예: "Pattern\: [...]")되어 있어도 java.util.Properties가 자동으로 이스케이프를 처리한다.
 * 반드시 UTF-8 Reader 오버로드로 읽어야 비ASCII 텍스트가 깨지지 않는다.
 */
public final class ParametersPropParser {

    private ParametersPropParser() {
    }

    public static Map<String, String> parse(byte[] propBytes) {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(propBytes), StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new ParserException("parameters.prop을 파싱할 수 없습니다.", e);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            values.put(key, props.getProperty(key));
        }
        return values;
    }
}
