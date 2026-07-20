package com.onetuks.iflow_sentinel.parser.wsdl;

import com.onetuks.iflow_sentinel.parser.TestFixtures;
import com.onetuks.iflow_sentinel.parser.model.SchemaArtifact;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZip;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WsdlParserTest {

    private final List<SchemaArtifact> schemas = ArtifactZip.from(TestFixtures.sampleArtifactZipBytes())
            .findAllByExtension(".wsdl").stream()
            .map(WsdlParser::parse)
            .toList();

    @Test
    void bothWsdlFilesAreParsed() {
        assertThat(schemas).hasSize(2);
        assertThat(schemas).extracting(SchemaArtifact::file).containsExactlyInAnyOrder("inbound.wsdl", "outbound.wsdl");
    }

    @Test
    void inboundWsdlHasCorrectNameAndMessageType() {
        SchemaArtifact inbound = findByFile(schemas, "inbound.wsdl");
        assertThat(inbound.name()).isEqualTo("GMES_GQMS_EA_PQCRESULT_01_AI");
        assertThat(inbound.targetNamespace()).isEqualTo("http://www.lgchem.com/GMES/GQMS");
        assertThat(inbound.messageTypes()).containsExactly("MT_GMES_GQMS_EA_PQCRESULT_01_T");
    }

    @Test
    void outboundWsdlHasCorrectNameAndMessageType() {
        SchemaArtifact outbound = findByFile(schemas, "outbound.wsdl");
        assertThat(outbound.name()).isEqualTo("GMES_GQMS_EA_PQCRESULT_01_AO");
        assertThat(outbound.messageTypes()).containsExactly("MT_GMES_GQMS_EA_PQCRESULT_01_S");
    }

    private static SchemaArtifact findByFile(List<SchemaArtifact> schemas, String file) {
        return schemas.stream().filter(s -> file.equals(s.file())).findFirst()
                .orElseThrow(() -> new AssertionError("파일을 찾을 수 없습니다: " + file));
    }
}
