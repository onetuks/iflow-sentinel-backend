package com.onetuks.iflow_sentinel.parser.model;

/**
 * 스크립트(Groovy/JS) 정보. isResolved=true면 ZIP 안에서 원본 파일을 찾아 source가 채워진 것이고,
 * isResolved=false면 steps[]가 참조하지만 원본이 외부 ScriptCollection에 있어 확보하지 못한 미해결 참조다
 * (source=null, 설계서 5.4 "미해결 참조는 검사 불가로 남긴다"의 근거 데이터).
 */
public record ScriptArtifact(String file, String language, String source, boolean isResolved) {
}
