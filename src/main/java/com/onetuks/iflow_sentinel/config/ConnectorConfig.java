package com.onetuks.iflow_sentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Connector가 사용하는 RestClient 빈. 테스트에서는 MockRestServiceServer로 바인딩한 RestClient로
 * 교체한다.
 * 연결/읽기 타임아웃을 명시하지 않으면 SAP 응답 지연 시 호출이 무한정 걸릴 수 있고, 아티팩트 동기화가
 * 순차 처리이므로 그 한 번의 지연이 이후 패키지 전체의 동기화를 계속 밀리게 만든다.
 */
@Configuration
public class ConnectorConfig {

    @Bean
    public RestClient restClient(
            @Value("${iflow-sentinel.connector.connect-timeout:10s}") Duration connectTimeout,
            @Value("${iflow-sentinel.connector.read-timeout:60s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
