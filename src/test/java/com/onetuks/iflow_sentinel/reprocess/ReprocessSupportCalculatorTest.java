package com.onetuks.iflow_sentinel.reprocess;

import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.ChannelAuth;
import com.onetuks.iflow_sentinel.parser.model.IflowConfig;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import com.onetuks.iflow_sentinel.parser.model.StoreRef;
import com.onetuks.iflow_sentinel.parser.util.ReprocessSupportCalculator;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReprocessSupportCalculatorTest {

    @Test
    @DisplayName("DataStore 스텝만 있는 경우 DATASTORE_ONLY를 반환한다")
    void calculateSupportType_dataStoreOnly() {
        // given
        StepNode storeStep = createStoreStep("MyDataStore", "90d");
        IflowModel model = createModel(List.of(), List.of(storeStep));

        // when
        ReprocessSupportType supportType = ReprocessSupportCalculator.calculateSupportType(model);

        // then
        assertThat(supportType).isEqualTo(ReprocessSupportType.DATASTORE_ONLY);
    }

    @Test
    @DisplayName("JMS 채널만 있는 경우 JMS_ONLY를 반환한다")
    void calculateSupportType_jmsOnly() {
        // given
        Channel jmsChannel = createJmsChannel("MyJmsQueue");
        IflowModel model = createModel(List.of(jmsChannel), List.of());

        // when
        ReprocessSupportType supportType = ReprocessSupportCalculator.calculateSupportType(model);

        // then
        assertThat(supportType).isEqualTo(ReprocessSupportType.JMS_ONLY);
    }

    @Test
    @DisplayName("DataStore 스텝과 JMS 채널이 모두 있는 경우 BOTH를 반환한다")
    void calculateSupportType_both() {
        // given
        StepNode storeStep = createStoreStep("MyDataStore", "30");
        Channel jmsChannel = createJmsChannel("MyJmsQueue");
        IflowModel model = createModel(List.of(jmsChannel), List.of(storeStep));

        // when
        ReprocessSupportType supportType = ReprocessSupportCalculator.calculateSupportType(model);

        // then
        assertThat(supportType).isEqualTo(ReprocessSupportType.BOTH);
    }

    @Test
    @DisplayName("둘 다 없는 경우 NONE을 반환한다")
    void calculateSupportType_none() {
        // given
        IflowModel model = createModel(List.of(), List.of());

        // when
        ReprocessSupportType supportType = ReprocessSupportCalculator.calculateSupportType(model);

        // then
        assertThat(supportType).isEqualTo(ReprocessSupportType.NONE);
    }

    @Test
    @DisplayName("expire 일수를 숫자만 정확히 추출한다")
    void parseExpireDays() {
        assertThat(ReprocessSupportCalculator.parseExpireDays("90d")).isEqualTo(90);
        assertThat(ReprocessSupportCalculator.parseExpireDays("30")).isEqualTo(30);
        assertThat(ReprocessSupportCalculator.parseExpireDays("180 days")).isEqualTo(180);
        assertThat(ReprocessSupportCalculator.parseExpireDays(null)).isNull();
    }

    @Test
    @DisplayName("DataStore 이름과 보존 일수를 추출한다")
    void extractDataStoreInfo() {
        // given
        StepNode storeStep = createStoreStep("TestStore", "60d");
        IflowModel model = createModel(List.of(), List.of(storeStep));

        // when
        Optional<ReprocessSupportCalculator.StoreInfo> info = ReprocessSupportCalculator.extractDataStoreInfo(model);

        // then
        assertThat(info).isPresent();
        assertThat(info.get().name()).isEqualTo("TestStore");
        assertThat(info.get().expireDays()).isEqualTo(60);
    }

    private StepNode createStoreStep(String storeName, String expire) {
        StoreRef storeRef = new StoreRef("get", storeName, "global", "false", expire, "msg1");
        return new StepNode("step1", "StoreStep", "DBstorage", "p1", "1.0", Map.of(), null, null, null, storeRef, null);
    }

    private Channel createJmsChannel(String queueName) {
        return new Channel(
                "ch1", "JmsChannel", "s", "t", "inbound", "JMS", "protocol", "msgProtocol",
                "1.0", "system", queueName, new ChannelAuth(null, null, null, null), List.of(),
                Map.of("ComponentType", "JMS", "QueueName", queueName)
        );
    }

    private IflowModel createModel(List<Channel> channels, List<StepNode> steps) {
        return new IflowModel(
                new IflowConfig("Log", "false", "false", "None", "false", "*", "*", "1.0", Map.of()),
                List.of(), channels, List.of(), List.of(), List.of(), List.of(), steps, List.of()
        );
    }
}
