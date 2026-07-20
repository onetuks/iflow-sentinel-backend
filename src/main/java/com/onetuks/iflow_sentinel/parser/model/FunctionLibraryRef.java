package com.onetuks.iflow_sentinel.parser.model;

/** .mmap의 lnkRole(UsedFuncLib)에서 추출한 사용된 함수 라이브러리 참조. */
public record FunctionLibraryRef(String file, String packageName) {
}
