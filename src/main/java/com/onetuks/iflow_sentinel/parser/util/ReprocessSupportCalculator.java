package com.onetuks.iflow_sentinel.parser.util;

import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ReprocessSupportCalculator {

    private ReprocessSupportCalculator() {
    }

    public static ReprocessSupportType calculateSupportType(IflowModel iflowModel) {
        if (iflowModel == null) {
            return ReprocessSupportType.NONE;
        }

        boolean hasDataStore = hasDataStoreStep(iflowModel.steps());
        boolean hasJms = hasJmsChannel(iflowModel.channels());

        if (hasDataStore && hasJms) {
            return ReprocessSupportType.BOTH;
        } else if (hasDataStore) {
            return ReprocessSupportType.DATASTORE_ONLY;
        } else if (hasJms) {
            return ReprocessSupportType.JMS_ONLY;
        } else {
            return ReprocessSupportType.NONE;
        }
    }

    public static boolean hasDataStoreStep(List<StepNode> steps) {
        if (steps == null) {
            return false;
        }
        return steps.stream().anyMatch(step -> step.store() != null);
    }

    public static boolean hasJmsChannel(List<Channel> channels) {
        if (channels == null) {
            return false;
        }
        return channels.stream().anyMatch(ReprocessSupportCalculator::isJmsChannel);
    }

    public static boolean isJmsChannel(Channel channel) {
        if (channel == null) {
            return false;
        }
        String adapterType = channel.adapterType();
        if (adapterType != null && adapterType.toUpperCase().contains("JMS")) {
            return true;
        }
        if (channel.properties() != null) {
            String componentType = channel.properties().get("ComponentType");
            if (componentType != null && componentType.toUpperCase().contains("JMS")) {
                return true;
            }
        }
        return false;
    }

    public static Optional<StoreInfo> extractDataStoreInfo(IflowModel iflowModel) {
        if (iflowModel == null || iflowModel.steps() == null) {
            return Optional.empty();
        }
        return iflowModel.steps().stream()
                .filter(step -> step != null && step.store() != null)
                .map(step -> step.store())
                .findFirst()
                .map(store -> new StoreInfo(
                        store.name(),
                        parseExpireDays(store.expire())
                ));
    }

    public static Optional<String> extractJmsQueueName(IflowModel iflowModel) {
        if (iflowModel == null || iflowModel.channels() == null) {
            return Optional.empty();
        }
        return iflowModel.channels().stream()
                .filter(ReprocessSupportCalculator::isJmsChannel)
                .map(channel -> {
                    if (channel.address() != null && !channel.address().isBlank()) {
                        return channel.address();
                    }
                    if (channel.properties() != null) {
                        return getFirstNonBlank(
                                channel.properties().get("QueueName"),
                                channel.properties().get("queueName"),
                                channel.properties().get("Destination"),
                                channel.properties().get("destinationName"),
                                channel.properties().get("Queue"),
                                channel.properties().get("destination")
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst();
    }

    public static Integer parseExpireDays(String expireStr) {
        if (expireStr == null || expireStr.isBlank()) {
            return null;
        }
        try {
            // "90", "90d", "90 days" 등 숫자만 정규식으로 추출
            String digits = expireStr.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return null;
            }
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getFirstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String val : values) {
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    public record StoreInfo(String name, Integer expireDays) {
    }
}
