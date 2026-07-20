package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import java.util.List;
import java.util.Map;

/** Rule.target()/params() (Map&lt;String,Object&gt;, JSON 저장)을 안전하게 읽기 위한 공용 헬퍼. */
final class RuleParams {

    private RuleParams() {
    }

    static Map<String, Object> orEmpty(Map<String, Object> map) {
        return map == null ? Map.of() : map;
    }

    static String string(Map<String, Object> map, String key) {
        Object value = orEmpty(map).get(key);
        return value == null ? null : value.toString();
    }

    static List<String> stringList(Map<String, Object> map, String key) {
        Object value = orEmpty(map).get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
