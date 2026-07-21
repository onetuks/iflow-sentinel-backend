package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.parser.ParserFacade;
import com.onetuks.iflow_sentinel.parser.model.ArtifactInfo;
import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.IflowConfig;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.rule.domain.Rule;
import com.onetuks.iflow_sentinel.rule.domain.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * RuleEngine evaluator 테스트 공용 헬퍼: 실제 Parser 픽스처 로딩 + 최소 ParsedModel/Rule 빌더.
 */
final class RuleEngineTestSupport {

    private RuleEngineTestSupport() {
    }

    /** 실제 SAP IS 샘플 아티팩트(GMES_GQMS_EA_PQCRESULT_01)를 Parser로 파싱한 진짜 ParsedModel. */
    static ParsedModel realFixtureModel() {
        try (InputStream in = RuleEngineTestSupport.class
                .getResourceAsStream("/parser/GMES_GQMS_EA_PQCRESULT_01.zip")) {
            if (in == null) {
                throw new IllegalStateException("테스트 픽스처를 찾을 수 없습니다: /parser/GMES_GQMS_EA_PQCRESULT_01.zip");
            }
            return new ParserFacade().parse(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Artifact artifact() {
        IntegrationPackage integrationPackage = IntegrationPackage.builder()
                .sapPackageId("PKG1").name("Test Package").build();
        return Artifact.builder()
                .integrationPackage(integrationPackage)
                .sapArtifactId("ART1")
                .name("Test Artifact")
                .version("1.0.0")
                .type(ArtifactType.IFLOW)
                .build();
    }

    static Rule rule(RuleType type, Map<String, Object> target, Map<String, Object> params, String message) {
        return Rule.builder()
                .ruleKey("test-rule")
                .type(type)
                .severity(Severity.FAIL)
                .target(target)
                .params(params)
                .message(message)
                .enabled(true)
                .build();
    }

    /**
     * channels[]만 채우고 나머지는 빈 값으로 채운 최소 ParsedModel. 실제 픽스처가 커버하지 못하는
     * 경우(ProcessDirect 등) 전용.
     */
    static ParsedModel modelWithChannels(List<Channel> channels) {
        IflowConfig config = new IflowConfig(null, null, null, null, null, null, null, null, Map.of());
        IflowModel iflow = new IflowModel(config, List.of(), channels, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
        ArtifactInfo artifactInfo = new ArtifactInfo("Test", "Test", "1.0.0", "IntegrationFlow", "iflmap", "", 0L,
                List.of());
        return new ParsedModel(1, artifactInfo, iflow, List.of(), List.of(), List.of(), List.of());
    }
}
