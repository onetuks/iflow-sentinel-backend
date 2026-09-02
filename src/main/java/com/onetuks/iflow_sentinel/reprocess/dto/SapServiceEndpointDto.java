package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
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
     * 실제 호출 가능한 엔드포인트 URL 또는 상대 경로를 추출한다. {@link #resolveAllUrls()}가 찾은 후보 중
     * 첫 번째(가장 우선순위 높은) 값이다.
     * 1순위: EntryPoints 내 Url
     * 2순위: DTO 자체 Url 필드
     * 3순위: Id 필드 내 '$endpointAddress=' 값 파싱 (예: MM2110_CPIX_ERP$endpointAddress=MM2110 -> /http/MM2110 또는 /MM2110)
     */
    public String resolveUrl() {
        List<String> urls = resolveAllUrls();
        return urls.isEmpty() ? null : urls.get(0);
    }

    /**
     * 호출 가능한 엔드포인트 URL 후보를 전부 추출한다. 하나의 iFlow가 여러 EntryPoints(예: 복수 버전,
     * 복수 Sender 채널)를 가진 경우, 재처리 시 순서대로 재시도하기 위해 사용한다.
     * 1순위: EntryPoints 내 모든 Url (중복 제거)
     * 2순위: DTO 자체 Url 필드
     * 3순위: Id 필드 내 '$endpointAddress=' 값 파싱
     */
    public List<String> resolveAllUrls() {
        List<String> urls = new ArrayList<>();
        if (EntryPoints != null && EntryPoints.results() != null) {
            for (SapEntryPointDto ep : EntryPoints.results()) {
                if (ep.Url() != null && !ep.Url().isBlank()) {
                    String trimmed = ep.Url().trim();
                    if (!urls.contains(trimmed)) {
                        urls.add(trimmed);
                    }
                }
            }
        }
        if (!urls.isEmpty()) {
            return urls;
        }
        if (Url != null && !Url.isBlank()) {
            return List.of(Url.trim());
        }
        if (Id != null && Id.contains("$endpointAddress=")) {
            String address = Id.substring(Id.indexOf("$endpointAddress=") + "$endpointAddress=".length()).trim();
            if (!address.isEmpty()) {
                if (address.startsWith("/")) {
                    return List.of(address);
                }
                // REST 또는 HTTP 프로토콜의 경우 CPI 기본 컨텍스트 /http/ 고려
                if ("REST".equalsIgnoreCase(Protocol) || "HTTP".equalsIgnoreCase(Protocol)) {
                    return List.of("/http/" + address);
                }
                return List.of("/" + address);
            }
        }
        return List.of();
    }
}
