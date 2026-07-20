package com.onetuks.iflow_sentinel.parser.mapping;

import com.onetuks.iflow_sentinel.parser.TestFixtures;
import com.onetuks.iflow_sentinel.parser.model.MappingArtifact;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZip;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MmapParserTest {

    private final MappingArtifact mapping = MmapParser.parse(
            ArtifactZip.from(TestFixtures.sampleArtifactZipBytes()).findAllByExtension(".mmap").get(0)
    );

    @Test
    void sourceAndTargetMessagesAreExtracted() {
        assertThat(mapping.sourceMessage().type()).isEqualTo("MT_GMES_GQMS_EA_PQCRESULT_01_S");
        assertThat(mapping.sourceMessage().namespace()).isEqualTo("http://www.lgchem.com/GMES/GQMS");
        assertThat(mapping.targetMessage().type()).isEqualTo("MT_GMES_GQMS_EA_PQCRESULT_01_T");
    }

    @Test
    void functionLibraryIsExtracted() {
        assertThat(mapping.functionLibraries()).hasSize(1);
        assertThat(mapping.functionLibraries().get(0).file()).isEqualTo("FL_GQMS.java");
    }

    @Test
    void functionsIncludeStandardAndCustom() {
        assertThat(mapping.functions())
                .anySatisfy(f -> {
                    assertThat(f.standard()).isTrue();
                    assertThat(f.name()).isEqualTo("const");
                });
        assertThat(mapping.functions())
                .anySatisfy(f -> {
                    assertThat(f.standard()).isFalse();
                    assertThat(f.name()).isEqualTo("FL_GQMS.String to NVARCHAR");
                    assertThat(f.library()).isEqualTo("FunctionalLibraries$FL_GQMS");
                });
    }

    @Test
    void fieldCountIsAggregatedFromBricks() {
        assertThat(mapping.fieldCount().source()).isGreaterThan(0);
        assertThat(mapping.fieldCount().target()).isGreaterThan(0);
    }
}
