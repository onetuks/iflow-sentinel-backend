package com.onetuks.iflow_sentinel.auth;

import com.onetuks.iflow_sentinel.auth.domain.user.Role;
import com.onetuks.iflow_sentinel.auth.domain.user.User;
import com.onetuks.iflow_sentinel.auth.domain.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 최초 구동 시 admin 계정이 없으면 기본 관리자 계정을 생성한다. */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "inspien$01";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(DEFAULT_ADMIN_USERNAME)) {
            return;
        }
        userRepository.save(User.builder()
                .username(DEFAULT_ADMIN_USERNAME)
                .passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .build());
        log.info("[AUTH] 기본 관리자 계정('{}')이 생성되었습니다.", DEFAULT_ADMIN_USERNAME);
    }
}
