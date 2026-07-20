package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/** activityType=Enricher(Content Modifier) 스텝의 정규화 필드. */
public record EnricherRef(String bodyType, List<EnricherRow> headerTable, List<EnricherRow> propertyTable) {
    public EnricherRef {
        headerTable = List.copyOf(headerTable);
        propertyTable = List.copyOf(propertyTable);
    }
}
