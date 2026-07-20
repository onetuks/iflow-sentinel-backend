package com.onetuks.iflow_sentinel.parser.model;

/** .mmap의 lnkRole(SOURCE_IFR_MESS/TARGET_IFR_MESS)에서 추출한 원본/대상 메시지 타입. */
public record MessageRef(String file, String type, String namespace) {
}
