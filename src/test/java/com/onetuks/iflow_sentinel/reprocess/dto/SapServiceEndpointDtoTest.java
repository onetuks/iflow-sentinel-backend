package com.onetuks.iflow_sentinel.reprocess.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SapServiceEndpointDtoTest {

    @Test
    @DisplayName("EntryPoints에 유효한 Url이 포함되어 있으면 최우선으로 반환한다")
    void resolveUrl_fromEntryPoints() {
        SapEntryPointDto entryPoint = new SapEntryPointDto(
                "https://nanoh2o-is-dev.it-cpi015.cfapps.ap12.hana.ondemand.com/http/MM2110",
                "application/json",
                "POST"
        );
        SapServiceEndpointDto.SapEntryPointsWrapper wrapper =
                new SapServiceEndpointDto.SapEntryPointsWrapper(List.of(entryPoint));

        SapServiceEndpointDto dto = new SapServiceEndpointDto(
                "MM2110_CPIX_ERP$endpointAddress=MM2110",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "1.0.5",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "REST",
                "https://fallback.example.com/direct",
                wrapper
        );

        assertThat(dto.resolveUrl())
                .isEqualTo("https://nanoh2o-is-dev.it-cpi015.cfapps.ap12.hana.ondemand.com/http/MM2110");
    }

    @Test
    @DisplayName("EntryPoints가 없거나 비어있을 때 Url 필드가 있으면 Url 필드를 반환한다")
    void resolveUrl_fromDirectUrl() {
        SapServiceEndpointDto dto = new SapServiceEndpointDto(
                "MM2110_CPIX_ERP$endpointAddress=MM2110",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "1.0.5",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "REST",
                "https://fallback.example.com/direct",
                null
        );

        assertThat(dto.resolveUrl()).isEqualTo("https://fallback.example.com/direct");
    }

    @Test
    @DisplayName("EntryPoints 및 Url 필드가 없을 때 Id의 $endpointAddress 값을 파싱하여 REST 기본 경로를 반환한다")
    void resolveUrl_fromIdEndpointAddress_rest() {
        SapServiceEndpointDto dto = new SapServiceEndpointDto(
                "MM2110_CPIX_ERP$endpointAddress=MM2110",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "1.0.5",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "REST",
                null,
                null
        );

        assertThat(dto.resolveUrl()).isEqualTo("/http/MM2110");
    }

    @Test
    @DisplayName("Id에 endpointAddress가 슬래시로 시작하는 경우 그대로 반환한다")
    void resolveUrl_fromIdEndpointAddress_withSlash() {
        SapServiceEndpointDto dto = new SapServiceEndpointDto(
                "MM2110_CPIX_ERP$endpointAddress=/custom/path",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "1.0.5",
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                "HTTP",
                null,
                null
        );

        assertThat(dto.resolveUrl()).isEqualTo("/custom/path");
    }

    @Test
    @DisplayName("아무런 주소 정보가 없으면 null을 반환한다")
    void resolveUrl_null() {
        SapServiceEndpointDto dto = new SapServiceEndpointDto(
                "MM2110_CPIX_ERP",
                "MM2110_CPIX_ERP",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(dto.resolveUrl()).isNull();
    }
}
