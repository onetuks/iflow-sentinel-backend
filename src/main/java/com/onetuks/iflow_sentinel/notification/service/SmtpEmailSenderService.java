package com.onetuks.iflow_sentinel.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class SmtpEmailSenderService implements EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSenderService.class);

    private final JavaMailSender javaMailSender;
    private final String senderEmail;

    public SmtpEmailSenderService(
            JavaMailSender javaMailSender,
            @Value("${app.notification.sender-email:noreply-iflow-sentinel@company.com}") String senderEmail) {
        this.javaMailSender = javaMailSender;
        this.senderEmail = senderEmail;
    }

    @Override
    public void sendHtmlEmail(List<String> recipients, String subject, String htmlBody) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("메일 수신자 목록이 비어 있어 발송을 건너뜁니다. subject={}", subject);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(senderEmail);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
            log.info("이메일 발송 성공. recipients={}, subject={}", recipients, subject);
        } catch (Exception e) {
            log.error("이메일 발송 중 오류 발생. recipients={}, subject={}, error={}", recipients, subject, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
