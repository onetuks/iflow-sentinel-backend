package com.onetuks.iflow_sentinel.parser.model;

/** Enricher 스텝의 headerTable/propertyTable 안에 중첩된 XML을 펼친 한 행. */
public record EnricherRow(
        String action,
        String type,
        String value,
        String defaultValue,
        String name,
        String datatype
) {
}
