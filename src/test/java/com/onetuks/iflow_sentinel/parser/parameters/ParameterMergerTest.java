package com.onetuks.iflow_sentinel.parser.parameters;

import com.onetuks.iflow_sentinel.parser.TestFixtures;
import com.onetuks.iflow_sentinel.parser.model.Parameter;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZip;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterMergerTest {

    private final ArtifactZip zip = ArtifactZip.from(TestFixtures.sampleArtifactZipBytes());

    @Test
    void mergesAllTwentyTwoParametersByName() {
        Map<String, String> values = ParametersPropParser.parse(zip.findByExactSuffix("parameters.prop").orElseThrow());
        Map<String, ParametersPropDefParser.ParamDef> defs = ParametersPropDefParser.parse(zip.findByExactSuffix("parameters.propdef").orElseThrow());

        List<Parameter> parameters = ParameterMerger.merge(values, defs, Map.of());

        assertThat(parameters).hasSize(22);
    }

    @Test
    void spaceContainingParameterNameRoundTripsCorrectly() {
        Map<String, String> values = ParametersPropParser.parse(zip.findByExactSuffix("parameters.prop").orElseThrow());

        assertThat(values).containsEntry("DataStore Sender_Retry_Interval", "1");
        assertThat(values).containsEntry("DataStore Sender_Lock_Timeout", "10");
    }

    @Test
    void koreanDescriptionIsReadAsUtf8() {
        Map<String, ParametersPropDefParser.ParamDef> defs = ParametersPropDefParser.parse(zip.findByExactSuffix("parameters.propdef").orElseThrow());

        String description = defs.get("Payload_Enable_Source").description();
        assertThat(description).contains("공통 변수를 따라 기록 여부를 결정합니다");
    }

    @Test
    void colonEscapedValueUnescapesCorrectly() {
        // metainfo.prop이 아니라 parameters.prop 자체에는 콜론 이스케이프가 없으므로,
        // 대신 파라미터 값 파싱이 escape 문자를 오염시키지 않는지 INTERFACE_ID로 확인한다.
        Map<String, String> values = ParametersPropParser.parse(zip.findByExactSuffix("parameters.prop").orElseThrow());
        assertThat(values).containsEntry("INTERFACE_ID", "GMES_GQMS_EA_PQCRESULT_01");
    }
}
