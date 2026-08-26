package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * SAP CPI OData /ServiceEndpoints 엔티티 매핑 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapServiceEndpointDto(
        String Id,
        String Name,
        String Title,
        String Version,
        String Summary,
        String Description,
        String Protocol,
        String Url,
        SapEntryPointsWrapper EntryPoints
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SapEntryPointsWrapper(
            List<SapEntryPointDto> results
    ) {
    }

    /**
     * 실제 호출 가능한 엔드포인트 URL 또는 상대 경로를 추출한다.
     * 1순위: EntryPoints 내 Url
     * 2순위: DTO 자체 Url 필드
     * 3순위: Id 필드 내 '$endpointAddress=' 값 파싱 (예: MM2110_CPIX_ERP$endpointAddress=MM2110 -> /http/MM2110 또는 /MM2110)
     */
    public String resolveUrl() {
        if (EntryPoints != null && EntryPoints.results() != null && !EntryPoints.results().isEmpty()) {
            for (SapEntryPointDto ep : EntryPoints.results()) {
                if (ep.Url() != null && !ep.Url().isBlank()) {
                    return ep.Url().trim();
                }
            }
        }
        if (Url != null && !Url.isBlank()) {
            return Url.trim();
        }
        if (Id != null && Id.contains("$endpointAddress=")) {
            String address = Id.substring(Id.indexOf("$endpointAddress=") + "$endpointAddress=".length()).trim();
            if (!address.isEmpty()) {
                if (address.startsWith("/")) {
                    return address;
                }
                // REST 또는 HTTP 프로토콜의 경우 CPI 기본 컨텍스트 /http/ 고려
                if ("REST".equalsIgnoreCase(Protocol) || "HTTP".equalsIgnoreCase(Protocol)) {
                    return "/http/" + address;
                }
                return "/" + address;
            }
        }
        return null;
    }
}
