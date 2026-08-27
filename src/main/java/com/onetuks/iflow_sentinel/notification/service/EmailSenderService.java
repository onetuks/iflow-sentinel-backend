package com.onetuks.iflow_sentinel.notification.service;

import java.util.List;

public interface EmailSenderService {

    /**
     * 지정된 수신자 목록으로 HTML 이메일을 발송합니다.
     *
     * @param recipients 수신자 이메일 목록
     * @param subject    메일 제목
     * @param htmlBody   HTML 메일 본문
     */
    void sendHtmlEmail(List<String> recipients, String subject, String htmlBody);
}
