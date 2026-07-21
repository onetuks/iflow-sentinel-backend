package com.onetuks.iflow_sentinel.rule.domain;

public enum Severity {
    FAIL("fail"),
    WARN("warn"),
    INFO("info");

    private final String code;

    Severity(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Severity fromCode(String code) {
        for (Severity severity : values()) {
            if (severity.code.equals(code)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown severity code: " + code);
    }
}
