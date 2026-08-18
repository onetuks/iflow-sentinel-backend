package com.onetuks.iflow_sentinel.connector.component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PackageZipParser {

    private static final Logger log = LoggerFactory.getLogger(PackageZipParser.class);
    private final ObjectMapper objectMapper;

    public PackageZipParser() {
        this.objectMapper = new ObjectMapper();
    }

    public PackageZipParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public record ParsedArtifactDefaultProperties(
            String id,
            String name,
            String version,
            Map<String, String> defaultValues) {
    }

    private record ArtifactResourceMeta(
            String id,
            String name,
            String uniqueId,
            String semanticVersion,
            String resourceType) {
    }

    /**
     * Package Export Zip ($value) 바이너리를 파싱하여 아티팩트별 Default Properties 정보 반환
     *
     * @param packageZipBytes Package Export Zip 바이너리
     * @return Map<ResourceUniqueId/Name, ParsedArtifactDefaultProperties>
     */
    public Map<String, ParsedArtifactDefaultProperties> parsePackageZip(byte[] packageZipBytes) {
        Map<String, ArtifactResourceMeta> metadataMap = new HashMap<>();
        Map<String, byte[]> contentFileMap = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(packageZipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("resources.cnt".equals(name)) {
                    byte[] bytes = zis.readAllBytes();
                    metadataMap = parseResourcesCnt(bytes);
                } else if (name.endsWith("_content")) {
                    byte[] bytes = zis.readAllBytes();
                    contentFileMap.put(name, bytes);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Package Export Zip 파싱 중 오류 발생", e);
            throw new IllegalArgumentException("Package Export ZIP 파싱 실패", e);
        }

        Map<String, ParsedArtifactDefaultProperties> result = new HashMap<>();

        for (Map.Entry<String, ArtifactResourceMeta> metaEntry : metadataMap.entrySet()) {
            ArtifactResourceMeta meta = metaEntry.getValue();
            String contentFileName = meta.id() + "_content";

            byte[] contentZipBytes = contentFileMap.get(contentFileName);
            if (contentZipBytes == null) {
                log.warn("아티팩트 {} ({})의 content 파일을 찾을 수 없습니다.", meta.uniqueId(), contentFileName);
                continue;
            }

            Map<String, String> defaultValues = extractDefaultValuesFromArtifactContentZip(contentZipBytes);

            ParsedArtifactDefaultProperties parsed = new ParsedArtifactDefaultProperties(
                    meta.id(),
                    meta.uniqueId(),
                    meta.semanticVersion(),
                    defaultValues);

            // uniqueId(예: Property_Test) 및 id(예: 5185fc...) 모두 키로 찾을 수 있도록 저장
            result.put(meta.uniqueId(), parsed);
            result.put(meta.id(), parsed);
        }

        return result;
    }

    private Map<String, ArtifactResourceMeta> parseResourcesCnt(byte[] cntBytes) throws IOException {
        String base64Content = new String(cntBytes, StandardCharsets.UTF_8).trim();
        byte[] jsonBytes = Base64.getDecoder().decode(base64Content);
        JsonNode root = objectMapper.readTree(jsonBytes);

        Map<String, ArtifactResourceMeta> map = new HashMap<>();
        JsonNode resources = root.path("resources");
        if (resources.isArray()) {
            for (JsonNode res : resources) {
                String id = res.path("id").stringValue();
                String name = res.path("name").stringValue();
                String uniqueId = res.path("uniqueId").stringValue(name);
                String semanticVersion = res.path("semanticVersion").stringValue("1.0.0");
                String resourceType = res.path("resourceType").stringValue();

                if (!id.isEmpty()) {
                    map.put(id, new ArtifactResourceMeta(id, name, uniqueId, semanticVersion, resourceType));
                }
            }
        }
        return map;
    }

    private Map<String, String> extractDefaultValuesFromArtifactContentZip(byte[] artifactZipBytes) {
        Map<String, String> propertiesMap = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(artifactZipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("src/main/resources/parameters.prop".equals(entry.getName())) {
                    Properties prop = new Properties();
                    prop.load(zis);
                    for (String key : prop.stringPropertyNames()) {
                        String val = prop.getProperty(key);
                        propertiesMap.put(key, val == null ? "" : val);
                    }
                    break;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("아티팩트 Content ZIP 내 parameters.prop 추출 실패", e);
        }
        return propertiesMap;
    }
}
