package com.onetuks.iflow_sentinel.parser.util;

import java.util.Locale;

/** 스크립트 파일 확장자로부터 언어를 파생하는 공용 헬퍼. StepParser와 ScriptFileCollector가 함께 사용한다. */
public final class ScriptLanguage {

    private ScriptLanguage() {
    }

    public static String fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".groovy")) {
            return "Groovy";
        }
        if (lower.endsWith(".js")) {
            return "JavaScript";
        }
        return null;
    }
}
