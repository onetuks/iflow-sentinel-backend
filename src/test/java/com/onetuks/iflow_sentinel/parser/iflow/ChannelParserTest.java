package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.TestFixtures;
import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelParserTest {

    private final IflowModel model = IflwParser.parse(TestFixtures.sampleIflwBytes()).model();

    @Test
    void senderHttpsChannelDerivesAddressAndAuthAndExternalizedRef() {
        Channel httpsChannel = findBySystem(model.channels(), "Sender1");
        assertThat(httpsChannel.direction()).isEqualTo("Sender");
        assertThat(httpsChannel.address()).isEqualTo("{{Sender_Address}}");
        assertThat(httpsChannel.externalizedRefs()).containsExactly("Sender_Address");
        assertThat(httpsChannel.auth().senderAuthType()).isEqualTo("RoleBased");
        assertThat(httpsChannel.auth().userRole()).isEqualTo("ESBMessaging.send");
    }

    @Test
    void dataStoreChannelCollectsAllExternalizedRefsIncludingSpaceContainingNames() {
        Channel dataStoreChannel = findBySystem(model.channels(), "Sender");
        assertThat(dataStoreChannel.externalizedRefs()).containsExactlyInAnyOrder(
                "Queue_DataStore_Name",
                "DataStore_Sender_Poll_Interval",
                "DataStore Sender_Retry_Interval",
                "DataStore Sender_Lock_Timeout"
        );
    }

    @Test
    void receiverHttpChannelHasNoneAuth() {
        Channel receiverChannel = findBySystem(model.channels(), "Receiver");
        assertThat(receiverChannel.direction()).isEqualTo("Receiver");
        assertThat(receiverChannel.auth().authenticationMethod()).isEqualTo("None");
        assertThat(receiverChannel.address()).contains("{{Receiver_Address}}");
    }

    @Test
    void everyChannelKeepsRawPropertiesMap() {
        assertThat(model.channels()).allSatisfy(channel -> {
            assertThat(channel.properties()).isNotEmpty();
            assertThat(channel.properties()).containsKey("cmdVariantUri");
        });
    }

    private static Channel findBySystem(List<Channel> channels, String system) {
        return channels.stream()
                .filter(c -> system.equals(c.system()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("system=" + system + "인 채널을 찾을 수 없습니다."));
    }
}
