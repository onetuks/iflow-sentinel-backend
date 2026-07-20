package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/**
 * parameters.prop(값)과 parameters.propdef(선언)를 이름 기준으로 병합한 외부화 파라미터.
 * referencedBy/isUsed는 .iflw 내 {{name}} 참조 위치를 대조해 Parser가 계산한 파생 값이다.
 */
public record Parameter(
        String name,
        String value,
        String type,
        boolean isRequired,
        String description,
        List<String> referencedBy,
        boolean isUsed
) {
    public Parameter {
        referencedBy = List.copyOf(referencedBy);
    }
}
