package com.onetuks.iflow_sentinel.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestEmailRequest(
        @NotBlank(message = "테스트 수신 이메일 주소는 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String targetEmail
) {
}
