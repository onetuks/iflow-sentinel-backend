package com.onetuks.iflow_sentinel.parser.model;

/** bpmn2:process에서 추출한 통합 프로세스/로컬 프로세스. type은 cmdVariantUri에서 파생한 컴포넌트 이름. */
public record ProcessNode(
        String id,
        String name,
        String type,
        String processType,
        String transactionalHandling,
        String transactionTimeout
) {
}
