package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.TestFixtures;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StepParserTest {

    private final IflowModel model = IflwParser.parse(TestFixtures.sampleIflwBytes()).model();

    @Test
    void mappingStepHasNormalizedMappingRef() {
        StepNode step = findByName(model.steps(), "MM_Request");
        assertThat(step.type()).isEqualTo("Mapping");
        assertThat(step.mapping()).isNotNull();
        assertThat(step.mapping().name()).isEqualTo("MM_GMES_GQMS_EA_PQCRESULT_01");
        assertThat(step.mapping().uri()).contains("MM_GMES_GQMS_EA_PQCRESULT_01.mmap");
        assertThat(step.mapping().reference()).isEqualTo("static");
        assertThat(step.script()).isNull();
    }

    @Test
    void scriptStepReferencesUnresolvedExternalFile() {
        StepNode step = findByName(model.steps(), "Check payloadMode");
        assertThat(step.type()).isEqualTo("Script");
        assertThat(step.script().bundleId()).isEqualTo("ScriptCollection_SMARTSHIFT");
        assertThat(step.script().file()).isEqualTo("Check PayloadMode.groovy");
        assertThat(step.script().language()).isEqualTo("Groovy");
    }

    @Test
    void enricherStepParsesPropertyTableRows() {
        StepNode step = findByName(model.steps(), "Init");
        assertThat(step.type()).isEqualTo("Enricher");
        assertThat(step.enricher().bodyType()).isEqualTo("expression");
        assertThat(step.enricher().propertyTable()).isNotEmpty();
        assertThat(step.enricher().propertyTable())
                .anySatisfy(row -> {
                    assertThat(row.name()).isEqualTo("DATE");
                    assertThat(row.type()).isEqualTo("expression");
                    assertThat(row.value()).isEqualTo("${date:now+9h:yyyy-MM-dd HH:mm:ss}");
                    assertThat(row.datatype()).isEqualTo("java.lang.String");
                });
        assertThat(step.enricher().headerTable())
                .anySatisfy(row -> assertThat(row.name()).isEqualTo("SAP_Sender"));
    }

    @Test
    void dbStorageStepHasNormalizedStoreRef() {
        StepNode step = findByName(model.steps(), "Sender Payload Write");
        assertThat(step.type()).isEqualTo("DBstorage");
        assertThat(step.store().operation()).isEqualTo("put");
        assertThat(step.store().name()).isEqualTo("{{DataStore_Name}}");
        assertThat(step.store().encrypt()).isEqualTo("true");
    }

    @Test
    void processCallElementStepHasNormalizedCallRef() {
        StepNode step = findByName(model.steps(), "Process Call 1");
        assertThat(step.type()).isEqualTo("ProcessCallElement");
        assertThat(step.call().processId()).isEqualTo("Process_7235237");
        assertThat(step.call().subActivityType()).isEqualTo("NonLoopingProcess");
    }

    @Test
    void serviceTaskWithoutActivityTypeDefaultsToExternalCall() {
        StepNode step = findByName(model.steps(), "Request Reply 1");
        assertThat(step.type()).isEqualTo("ExternalCall");
        assertThat(step.mapping()).isNull();
        assertThat(step.script()).isNull();
        assertThat(step.enricher()).isNull();
        assertThat(step.store()).isNull();
        assertThat(step.call()).isNull();
    }

    private static StepNode findByName(List<StepNode> steps, String name) {
        return steps.stream()
                .filter(s -> name.equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("스텝을 찾을 수 없습니다: " + name));
    }
}
