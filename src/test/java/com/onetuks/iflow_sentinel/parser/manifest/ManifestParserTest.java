package com.onetuks.iflow_sentinel.parser.manifest;

import com.onetuks.iflow_sentinel.parser.model.RequiredCapability;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인라인 MANIFEST.MF 픽스처로 72자 줄바꿈 연속 헤더(Require-Capability) unfold와
 * Require-Capability 절 파싱을 전체 ZIP 픽스처와 독립적으로 검증한다.
 */
class ManifestParserTest {

    @Test
    void unfoldsWrappedRequireCapabilityHeaderAndParsesClauses() {
        // 실제 MANIFEST.MF와 동일하게, 연속 라인은 단일 공백으로 시작한다.
        String manifest = String.join("\n",
                "Manifest-Version: 1.0",
                "Bundle-Name: TestArtifact",
                "Bundle-SymbolicName: TestArtifact",
                "Bundle-Version: 9.9.9",
                "SAP-BundleType: IntegrationFlow",
                "SAP-RuntimeProfile: iflmap",
                "Require-Capability: functionlibraries.FunctionalLibraries;resolution:",
                " =optional;bundleType:String=\"FunctionLibraries\";source:String=\"ref",
                " erence\", scriptcollection.ScriptCollection_TEST;resolution:=mandat",
                " ory",
                "Origin-ModifiedDate: 123456789",
                ""
        ) + "\n";

        ManifestParser.ManifestData data = ManifestParser.parse(manifest.getBytes(StandardCharsets.UTF_8));

        assertThat(data.name()).isEqualTo("TestArtifact");
        assertThat(data.version()).isEqualTo("9.9.9");
        assertThat(data.bundleType()).isEqualTo("IntegrationFlow");
        assertThat(data.modifiedAt()).isEqualTo(123456789L);

        assertThat(data.requiredCapabilities()).hasSize(2);
        assertThat(data.requiredCapabilities()).extracting(RequiredCapability::type)
                .containsExactlyInAnyOrder("functionlibraries", "scriptcollection");
        assertThat(data.requiredCapabilities()).extracting(RequiredCapability::name)
                .containsExactlyInAnyOrder("FunctionalLibraries", "ScriptCollection_TEST");
        assertThat(data.requiredCapabilities()).extracting(RequiredCapability::resolution)
                .containsExactlyInAnyOrder("optional", "mandatory");
    }
}
