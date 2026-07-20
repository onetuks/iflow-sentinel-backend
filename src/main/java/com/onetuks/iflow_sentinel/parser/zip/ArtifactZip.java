package com.onetuks.iflow_sentinel.parser.zip;

import com.onetuks.iflow_sentinel.parser.ParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 아티팩트 ZIP의 모든 엔트리를 메모리에 올려 파일명 접미사/확장자 기준으로 조회한다.
 * 아티팩트 ZIP은 MANIFEST 선언 경로와 실제 엔트리 경로(예: src/main/resources/... 접두어)가 다를 수 있으므로,
 * 절대 경로가 아니라 접미사/확장자로만 매칭한다.
 */
public final class ArtifactZip {

    private final Map<String, byte[]> entriesByPath;

    private ArtifactZip(Map<String, byte[]> entriesByPath) {
        this.entriesByPath = entriesByPath;
    }

    public static ArtifactZip from(byte[] zipBytes) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entries.put(entry.getName(), readAll(zis));
            }
        } catch (IOException e) {
            throw new ParserException("아티팩트 ZIP을 읽을 수 없습니다.", e);
        }
        if (entries.isEmpty()) {
            throw new ParserException("아티팩트 ZIP에 파일이 없습니다.");
        }
        return new ArtifactZip(entries);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);
        return out.toByteArray();
    }

    /** 경로가 주어진 접미사로 끝나는 단 하나의 엔트리를 조회한다. 없으면 empty, 둘 이상이면 예외. */
    public Optional<byte[]> findByExactSuffix(String suffix) {
        List<byte[]> matches = entriesByPath.entrySet().stream()
                .filter(e -> e.getKey().endsWith(suffix))
                .map(Map.Entry::getValue)
                .toList();
        if (matches.size() > 1) {
            throw new ParserException("접미사 '" + suffix + "'에 해당하는 엔트리가 둘 이상입니다.");
        }
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    /** 정확히 하나 존재해야 하는 확장자 파일을 조회한다(예: .iflw). 0개/2개 이상이면 예외. */
    public ArtifactZipEntry requireSingleByExtension(String extension) {
        List<ArtifactZipEntry> matches = findAllByExtension(extension);
        if (matches.isEmpty()) {
            throw new ParserException("확장자 '" + extension + "' 파일을 찾을 수 없습니다.");
        }
        if (matches.size() > 1) {
            throw new ParserException("확장자 '" + extension + "' 파일이 둘 이상 존재합니다.");
        }
        return matches.get(0);
    }

    /** 주어진 확장자들(대소문자 무시) 중 하나로 끝나는 모든 엔트리를 조회한다. 디렉터리 이름은 가정하지 않는다. */
    public List<ArtifactZipEntry> findAllByExtension(String... extensions) {
        return entriesByPath.entrySet().stream()
                .filter(e -> matchesAnyExtension(e.getKey(), extensions))
                .map(e -> new ArtifactZipEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private boolean matchesAnyExtension(String path, String[] extensions) {
        String lower = path.toLowerCase();
        for (String ext : extensions) {
            if (lower.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
