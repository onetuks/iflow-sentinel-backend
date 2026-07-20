package com.onetuks.iflow_sentinel.parser.zip;

/** ZIP 내 파일 하나(전체 경로 + 바이트 내용). */
public record ArtifactZipEntry(String path, byte[] content) {

    /** 경로의 마지막 세그먼트(디렉터리 제외 파일명)를 반환한다. */
    public String fileName() {
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return idx < 0 ? path : path.substring(idx + 1);
    }
}
