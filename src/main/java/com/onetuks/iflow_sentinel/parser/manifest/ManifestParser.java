package com.onetuks.iflow_sentinel.parser.manifest;

import com.onetuks.iflow_sentinel.parser.ParserException;
import com.onetuks.iflow_sentinel.parser.model.RequiredCapability;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * META-INF/MANIFEST.MF를 파싱한다. java.util.jar.Manifest를 사용해 72자 줄바꿈 연속 헤더
 * (Require-Capability, Import-Package 등)를 자동으로 unfold한다.
 */
public final class ManifestParser {

    private ManifestParser() {
    }

    public static ManifestData parse(byte[] manifestBytes) {
        Attributes attrs;
        try {
            Manifest mf = new Manifest(new ByteArrayInputStream(manifestBytes));
            attrs = mf.getMainAttributes();
        } catch (IOException e) {
            throw new ParserException("MANIFEST.MF를 파싱할 수 없습니다.", e);
        }

        String name = attrs.getValue("Bundle-Name");
        String symbolicName = attrs.getValue("Bundle-SymbolicName");
        String version = attrs.getValue("Bundle-Version");
        String bundleType = attrs.getValue("SAP-BundleType");
        String runtimeProfile = attrs.getValue("SAP-RuntimeProfile");
        String modifiedDateRaw = attrs.getValue("Origin-ModifiedDate");
        long modifiedAt = modifiedDateRaw == null || modifiedDateRaw.isBlank() ? 0L : Long.parseLong(modifiedDateRaw.trim());

        List<RequiredCapability> requiredCapabilities = parseRequireCapability(attrs.getValue("Require-Capability"));

        return new ManifestData(name, symbolicName, version, bundleType, runtimeProfile, modifiedAt, requiredCapabilities);
    }

    /**
     * Require-Capability 헤더는 절(clause)들을 콤마로, 각 절 안의 속성/지시어들을 세미콜론으로 구분한다.
     * 절 안의 따옴표로 감싼 속성값에는 콤마가 올 수 있으므로, 따옴표 안의 콤마는 분리 지점에서 제외한다.
     */
    private static List<RequiredCapability> parseRequireCapability(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        List<RequiredCapability> result = new ArrayList<>();
        for (String clause : splitTopLevel(header, ',')) {
            String trimmed = clause.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] segments = splitTopLevel(trimmed, ';').toArray(new String[0]);
            String namespace = segments[0].trim();
            String type = namespace;
            String name = namespace;
            int dot = namespace.indexOf('.');
            if (dot >= 0) {
                type = namespace.substring(0, dot);
                name = namespace.substring(dot + 1);
            }
            String resolution = "";
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i].trim();
                if (segment.startsWith("resolution")) {
                    int eq = segment.indexOf('=');
                    resolution = eq < 0 ? "" : unquote(segment.substring(eq + 1).trim());
                }
            }
            result.add(new RequiredCapability(type, name, resolution));
        }
        return result;
    }

    private static List<String> splitTopLevel(String text, char delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : text.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == delimiter && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** MANIFEST.MF에서 확보 가능한 아티팩트 메타의 일부. metainfo.prop의 description과 병합해 ArtifactInfo를 완성한다. */
    public record ManifestData(
            String name,
            String symbolicName,
            String version,
            String bundleType,
            String runtimeProfile,
            long modifiedAt,
            List<RequiredCapability> requiredCapabilities
    ) {
        public ManifestData {
            requiredCapabilities = List.copyOf(requiredCapabilities);
        }
    }
}
