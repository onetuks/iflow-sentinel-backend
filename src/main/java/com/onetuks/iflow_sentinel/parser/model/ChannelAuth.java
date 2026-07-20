package com.onetuks.iflow_sentinel.parser.model;

/** 채널의 인증 관련 ifl:property를 모은 파생 객체. sender/receiver 방향에 따라 채워지는 필드가 다르다. */
public record ChannelAuth(
        String senderAuthType,
        String userRole,
        String authenticationMethod,
        String credentialName
) {
}
