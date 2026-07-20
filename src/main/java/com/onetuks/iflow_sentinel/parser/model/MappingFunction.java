package com.onetuks.iflow_sentinel.parser.model;

/** .mmap의 &lt;brick type="Func"&gt;에서 추출. standard=false면 library에 사용된 함수 라이브러리 fns 값이 담긴다. */
public record MappingFunction(String name, boolean standard, String library) {
}
