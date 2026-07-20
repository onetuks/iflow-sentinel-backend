package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.TestFixtures;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import com.onetuks.iflow_sentinel.parser.model.Participant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantParserTest {

    private final IflowModel model = IflwParser.parse(TestFixtures.sampleIflwBytes()).model();

    @Test
    void receiverParticipantPreservesSapTypoButNormalizesRole() {
        Participant receiver = findByName(model.participants(), "Receiver");
        // SAP 원본 오탈자("EndpointRecevier")는 그대로 보존한다.
        assertThat(receiver.type()).isEqualTo("EndpointRecevier");
        // 파생 role은 정규화되어 "receiver"다.
        assertThat(receiver.role()).isEqualTo("receiver");
    }

    @Test
    void senderParticipantRole() {
        Participant sender = findByName(model.participants(), "Sender");
        assertThat(sender.type()).isEqualTo("EndpointSender");
        assertThat(sender.role()).isEqualTo("sender");
    }

    @Test
    void integrationProcessParticipantHasProcessRefAndProcessRole() {
        Participant mainProcess = findByName(model.participants(), "Main Process");
        assertThat(mainProcess.type()).isEqualTo("IntegrationProcess");
        assertThat(mainProcess.role()).isEqualTo("process");
        assertThat(mainProcess.processRef()).isEqualTo("Process_7706");
    }

    private static Participant findByName(List<Participant> participants, String name) {
        return participants.stream()
                .filter(p -> name.equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("참여자를 찾을 수 없습니다: " + name));
    }
}
