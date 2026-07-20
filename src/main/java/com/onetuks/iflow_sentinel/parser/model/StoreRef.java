package com.onetuks.iflow_sentinel.parser.model;

/** activityType=DBstorage 스텝의 정규화 필드. */
public record StoreRef(
        String operation,
        String name,
        String visibility,
        String encrypt,
        String expire,
        String messageId
) {
}
