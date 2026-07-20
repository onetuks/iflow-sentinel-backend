package com.onetuks.iflow_sentinel.parser.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {{paramName}} 형태의 외부화 파라미터 참조 토큰을 스캔하는 헬퍼. */
public final class TokenScanner {

    private static final Pattern EXTERNALIZED_REF = Pattern.compile("\\{\\{([^{}]+)}}");

    private TokenScanner() {
    }

    /** text 안에 등장하는 모든 {{name}} 토큰의 name을 처음 등장한 순서대로, 중복 없이 반환한다. */
    public static List<String> findExternalizedRefs(String text) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher matcher = EXTERNALIZED_REF.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return new ArrayList<>(found);
    }

    /**
     * text에서 발견된 각 {{name}} 토큰에 대해 target 맵에 elementId를 추가한다.
     * parameters[].referencedBy를 계산하기 위해 채널/스텝 파서가 각자 처리 중 호출하는 부가 산출 헬퍼.
     */
    public static void collectInto(Map<String, List<String>> target, String elementId, String text) {
        for (String token : findExternalizedRefs(text)) {
            target.computeIfAbsent(token, k -> new ArrayList<>()).add(elementId);
        }
    }
}
