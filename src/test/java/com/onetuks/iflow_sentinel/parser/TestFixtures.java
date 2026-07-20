package com.onetuks.iflow_sentinel.parser;

import com.onetuks.iflow_sentinel.parser.zip.ArtifactZip;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** 테스트 전용 픽스처 로더. 실제 SAP IS 아티팩트 샘플(GMES_GQMS_EA_PQCRESULT_01)을 공용으로 제공한다. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static byte[] sampleArtifactZipBytes() {
        try (InputStream in = TestFixtures.class.getResourceAsStream("/parser/GMES_GQMS_EA_PQCRESULT_01.zip")) {
            if (in == null) {
                throw new IllegalStateException("테스트 픽스처를 찾을 수 없습니다: /parser/GMES_GQMS_EA_PQCRESULT_01.zip");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] sampleIflwBytes() {
        return ArtifactZip.from(sampleArtifactZipBytes()).requireSingleByExtension(".iflw").content();
    }
}
