package com.onetuks.iflow_sentinel.rule.domain.rule;

public enum RuleType {
    NAMING_CONVENTION("naming-convention", RuleScope.SINGLE),
    REQUIRED_ERROR_HANDLER("required-error-handler", RuleScope.SINGLE),
    EXTERNALIZED_ENDPOINT("externalized-endpoint", RuleScope.SINGLE),
    ALLOWED_ADAPTER_TYPES("allowed-adapter-types", RuleScope.SINGLE),
    REQUIRED_LOGGING("required-logging", RuleScope.SINGLE),
    ALLOWED_SCRIPT_LANGUAGE("allowed-script-language", RuleScope.SINGLE),
    MAPPING_TYPE("mapping-type", RuleScope.SINGLE),
    REQUIRED_PARAMETER("required-parameter", RuleScope.SINGLE),
    FORBIDDEN_CONFIGURATION("forbidden-configuration", RuleScope.SINGLE),
    PROCESSDIRECT_PAIRING("processdirect-pairing", RuleScope.CROSS);

    private final String code;
    private final RuleScope scope;

    RuleType(String code, RuleScope scope) {
        this.code = code;
        this.scope = scope;
    }

    public String getCode() {
        return code;
    }

    public RuleScope getScope() {
        return scope;
    }

    public static RuleType fromCode(String code) {
        for (RuleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown rule type code: " + code);
    }
}
