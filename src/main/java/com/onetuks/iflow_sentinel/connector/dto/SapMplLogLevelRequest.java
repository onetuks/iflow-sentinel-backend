package com.onetuks.iflow_sentinel.connector.dto;

public record SapMplLogLevelRequest(
        String artifactSymbolicName,
        String mplLogLevel,
        String nodeType,
        String runtimeLocationId) {

    public static SapMplLogLevelRequest of(String artifactSymbolicName, String mplLogLevel) {
        return new SapMplLogLevelRequest(artifactSymbolicName, mplLogLevel, "IFLMAP", "cloudintegration");
    }
}
