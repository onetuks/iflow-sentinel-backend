package com.onetuks.iflow_sentinel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Connector가 사용하는 RestClient 빈. 테스트에서는 MockRestServiceServer로 바인딩한 RestClient로
 * 교체한다.
 */
@Configuration
public class ConnectorConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
