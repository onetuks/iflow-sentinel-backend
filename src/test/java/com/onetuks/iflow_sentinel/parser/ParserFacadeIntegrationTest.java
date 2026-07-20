package com.onetuks.iflow_sentinel.parser;

import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.parser.model.RequiredCapability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 SAP IS 아티팩트 샘플(GMES_GQMS_EA_PQCRESULT_01, Bundle-Version 1.0.5)을 기준으로
 * ParsedModel 전체를 검증한다. 설계서 5장이 이 샘플을 해체해 확인한 사실에 근거하므로,
 * 여기 있는 값들은 임의의 기대치가 아니라 실측값이다.
 */
class ParserFacadeIntegrationTest {

    private static ParsedModel model;

    @BeforeAll
    static void parseFixture() {
        model = new ParserFacade().parse(TestFixtures.sampleArtifactZipBytes());
    }

    @Test
    void schemaVersionIsOne() {
        assertThat(model.schemaVersion()).isEqualTo(1);
    }

    @Test
    void artifactMetadataMatchesManifest() {
        assertThat(model.artifact().name()).isEqualTo("GMES_GQMS_EA_PQCRESULT_01");
        assertThat(model.artifact().symbolicName()).isEqualTo("GMES_GQMS_EA_PQCRESULT_01");
        assertThat(model.artifact().version()).isEqualTo("1.0.5");
        assertThat(model.artifact().bundleType()).isEqualTo("IntegrationFlow");
        assertThat(model.artifact().runtimeProfile()).isEqualTo("iflmap");
        assertThat(model.artifact().modifiedAt()).isEqualTo(1782376597115L);
        assertThat(model.artifact().description()).contains("P01700_HTTP_HTTP_Async");
    }

    @Test
    void requiredCapabilitiesAreParsedFromManifest() {
        assertThat(model.artifact().requiredCapabilities()).hasSize(2);
        assertThat(model.artifact().requiredCapabilities())
                .extracting(RequiredCapability::name)
                .containsExactlyInAnyOrder("ScriptCollection_SMARTSHIFT", "FunctionalLibraries");
        assertThat(model.artifact().requiredCapabilities())
                .allMatch(cap -> "optional".equals(cap.resolution()));
    }

    @Test
    void exceptionSubprocessesIsEmptyNotNull() {
        // 설계서 5.3.7의 실증 사례 회귀 방지: 이 아티팩트는 예외 처리 서브프로세스가 0개다.
        assertThat(model.iflow().exceptionSubprocesses()).isNotNull().isEmpty();
    }

    @Test
    void allTwentyTwoParametersAreMergedAndUsed() {
        assertThat(model.parameters()).hasSize(22);
        assertThat(model.parameters()).allMatch(p -> p.type() != null && !p.type().isBlank());
        // 설계서 5.3.8의 실증 사례: 선언·설정·참조 22개가 모두 일치해 미사용 파라미터가 없다.
        assertThat(model.parameters()).allMatch(p -> p.isUsed());
    }

    @Test
    void scriptsContainsUnresolvedReference() {
        // ScriptCollection_SMARTSHIFT의 Check PayloadMode.groovy는 ZIP에 없으므로 미해결로 남아야 한다.
        assertThat(model.scripts()).hasSize(1);
        assertThat(model.scripts().get(0).file()).isEqualTo("Check PayloadMode.groovy");
        assertThat(model.scripts().get(0).isResolved()).isFalse();
        assertThat(model.scripts().get(0).source()).isNull();
        assertThat(model.scripts().get(0).language()).isEqualTo("Groovy");
    }

    @Test
    void mappingsAndSchemasAreCollected() {
        assertThat(model.mappings()).hasSize(1);
        assertThat(model.schemas()).hasSize(2);
    }
}
