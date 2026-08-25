package com.onetuks.iflow_sentinel.connector.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class PackageZipParserTest {

    private final PackageZipParser packageZipParser = new PackageZipParser(new ObjectMapper());

    @Test
    @DisplayName("Package Export ZIP 파일 파싱 테스트 - references/TEST_10192_A.zip")
    void parsePackageZip_Test() throws IOException {
        // given
        File sampleZipFile = new File("references/TEST_10192_A.zip");
        org.junit.jupiter.api.Assumptions.assumeTrue(sampleZipFile.exists(), "샘플 ZIP 파일이 존재하지 않아 테스트를 건너뜁니다.");
        byte[] packageZipBytes = Files.readAllBytes(sampleZipFile.toPath());

        // when
        Map<String, PackageZipParser.ParsedArtifactDefaultProperties> result = packageZipParser
                .parsePackageZip(packageZipBytes);

        // then
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result).containsKey("Property_Test");

        PackageZipParser.ParsedArtifactDefaultProperties artifactProps = result.get("Property_Test");
        assertThat(artifactProps.name()).isEqualTo("Property_Test");
        assertThat(artifactProps.defaultValues()).isNotNull();
        assertThat(artifactProps.defaultValues()).containsKeys("Receiver_Address", "Receiver_Credential");
    }
}
