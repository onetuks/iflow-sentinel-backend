package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/** MANIFEST.MF + metainfo.prop에서 추출한 아티팩트 메타데이터. */
public record ArtifactInfo(
        String name,
        String symbolicName,
        String version,
        String bundleType,
        String runtimeProfile,
        String description,
        long modifiedAt,
        List<RequiredCapability> requiredCapabilities
) {
    public ArtifactInfo {
        requiredCapabilities = List.copyOf(requiredCapabilities);
    }
}
