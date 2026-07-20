package com.onetuks.iflow_sentinel.parser.model;

/** MANIFEST.MF의 Require-Capability 절 하나. 이 iFlow가 의존하는 외부 아티팩트 선언. */
public record RequiredCapability(String type, String name, String resolution) {
}
