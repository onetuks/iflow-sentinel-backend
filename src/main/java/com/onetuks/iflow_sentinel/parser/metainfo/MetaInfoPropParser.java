package com.onetuks.iflow_sentinel.parser.metainfo;

import com.onetuks.iflow_sentinel.parser.ParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * metainfo.prop을 파싱해 아티팩트 설명을 추출한다. 한글 등 비ASCII 텍스트가 포함되므로
 * 반드시 UTF-8 InputStreamReader로 읽어야 한다(Properties.load(InputStream) 바이트 오버로드는
 * 기본적으로 ISO-8859-1로 해석해 한글 설명이 깨진다).
 */
public final class MetaInfoPropParser {

    private MetaInfoPropParser() {
    }

    public static String parseDescription(byte[] metaInfoPropBytes) {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(metaInfoPropBytes), StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new ParserException("metainfo.prop을 파싱할 수 없습니다.", e);
        }
        return props.getProperty("description", "");
    }
}
