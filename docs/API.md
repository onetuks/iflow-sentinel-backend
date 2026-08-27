# iFlow Sentinel Backend API 문서

Controller 클래스를 기준으로 정리한 API 명세입니다. Base path는 없으며, 각 엔드포인트는 아래 표기된 경로를 그대로 사용합니다.

## 공통 사항

### 에러 응답

모든 API는 예외 발생 시 아래 형식의 JSON을 반환합니다. (`GlobalExceptionHandler` 기준)

```json
{
  "message": "에러 메시지"
}
```

| 상황 | HTTP 상태 코드 |
|---|---|
| 리소스를 찾을 수 없음 (`NoSuchElementException`) | 404 Not Found |
| SAP 연동 오류 (`ConnectorException`) | 예외의 `statusCode`가 400~599면 그대로 사용, 아니면 502 Bad Gateway |
| 아티팩트 파싱 오류 (`ParserException`) | 422 Unprocessable Entity |

### 공통 Enum

| Enum | 값 |
|---|---|
| `Severity` | `FAIL`, `WARN`, `INFO` |
| `RuleType` | `NAMING_CONVENTION`, `REQUIRED_ERROR_HANDLER`, `EXTERNALIZED_ENDPOINT`, `ALLOWED_ADAPTER_TYPES`, `REQUIRED_LOGGING`, `ALLOWED_SCRIPT_LANGUAGE`, `MAPPING_TYPE`, `REQUIRED_PARAMETER`, `FORBIDDEN_CONFIGURATION`, `PROCESSDIRECT_PAIRING` |
| `TenantPlatform` | `NEO`, `CLOUD_FOUNDRY` |
| `TenantAuthType` | `OAUTH2_CLIENT_CREDENTIALS` |
| `ArtifactType` | `IFLOW`, `MESSAGE_MAPPING`, `VALUE_MAPPING`, `SCRIPT_COLLECTION`, `FUNCTION_LIBRARY` |
| `CheckRunStatus` | `RUNNING`, `COMPLETED`, `FAILED` |
| `LogLevel` | `NONE`, `INFO`, `ERROR`, `DEBUG`, `TRACE` |

---

## 1. Project API

`ProjectController` — `/api/projects`

### 프로젝트 생성
`POST /api/projects`

Request Body (`ProjectRequest`)
```json
{ "name": "string" }
```

Response (`ProjectResponse`)
```json
{ "id": 1, "name": "string" }
```

### 프로젝트 목록 조회
`GET /api/projects`

Response: `ProjectResponse[]`

### 프로젝트 수정
`PUT /api/projects/{id}`

Request Body: `ProjectRequest` (위와 동일)
Response: `ProjectResponse`

### 프로젝트 삭제
`DELETE /api/projects/{id}`

Response: 없음 (200 OK, body 없음)

---

## 2. Project Rule API

`ProjectRuleController` — `/api/projects/{projectId}/rules`

프로젝트에 적용 가능한 규칙(전역 + 프로젝트 전용) 목록과 활성화 여부를 관리합니다.

### 적용 가능한 규칙 목록 조회
`GET /api/projects/{projectId}/rules`

Response (`ProjectRuleResponse[]`)
```json
[
  {
    "ruleId": 1,
    "ruleKey": "string",
    "isGlobal": true,
    "type": "NAMING_CONVENTION",
    "severity": "FAIL",
    "message": "string",
    "isEnabled": true
  }
]
```

### 규칙 활성화 여부 변경
`PUT /api/projects/{projectId}/rules/{ruleId}`

Request Body (`ProjectRuleUpdateRequest`)
```json
{ "isEnabled": true }
```

Response: `ProjectRuleResponse` (위와 동일 형태)

---

## 3. Tenant API

`TenantController` — `/api/tenants`

SAP Integration Suite 테넌트(연결 정보) 관리.

### 테넌트 생성
`POST /api/tenants`

테넌트 저장 직후 SAP에서 Integration Package 목록을 자동으로 동기화하여 DB에 적재합니다. ([Integration Package API](#4-integration-package-api) 참고)

Request Body (`TenantRequest`)
```json
{
  "projectId": 1,
  "name": "string",
  "odataUrl": "string",
  "tokenUrl": "string",
  "platformType": "NEO | CLOUD_FOUNDRY",
  "authType": "OAUTH2_CLIENT_CREDENTIALS",
  "clientId": "string",
  "clientSecret": "string",
  "interfaceUrl": "string (선택: iFlow 런타임 호출 기본 URL, 예: https://<subaccount>-rt.cfapps...)",
  "interfaceTokenUrl": "string (선택: iFlow 런타임 호출 전용 OAuth2 토큰 발급 URL)",
  "interfaceAuthType": "BASIC | OAUTH2_CLIENT_CREDENTIALS (선택: 기본 BASIC)",
  "interfaceUsername": "string (선택: iFlow 호출용 사용자명 또는 Client ID)",
  "interfacePassword": "string (선택: iFlow 호출용 비밀번호 또는 Client Secret)"
}
```

Response (`TenantResponse`) — `clientSecret` 및 `interfacePassword`는 응답에 포함되지 않음
```json
{
  "id": 1,
  "projectId": 1,
  "name": "string",
  "odataUrl": "string",
  "tokenUrl": "string",
  "platformType": "NEO",
  "authType": "OAUTH2_CLIENT_CREDENTIALS",
  "clientId": "string",
  "interfaceUrl": "string",
  "interfaceTokenUrl": "string",
  "interfaceAuthType": "BASIC",
  "interfaceUsername": "string",
  "status": "connected",
  "packageCount": 0
}
```

### 테넌트 목록 조회
`GET /api/tenants?projectId={projectId}`

- `projectId`: 선택. 미지정 시 전체 테넌트 조회.

Response: `TenantResponse[]`

### 테넌트 단건 조회
`GET /api/tenants/{id}`

Response: `TenantResponse`

### 연결 테스트
`POST /api/tenants/{id}/test-connection`

Response (`ConnectionTestResult`) — `statusCode`는 네트워크 오류 등 HTTP 응답이 없는 경우 `-1`
```json
{ "success": true, "statusCode": 200, "message": "string" }
```

### 테넌트 수정
`PUT /api/tenants/{id}`

Request Body: `TenantRequest` (위와 동일)
Response: `TenantResponse`

### 테넌트 삭제
`DELETE /api/tenants/{id}`

Response: 없음

### 테넌트 로그 레벨 설정 조회
`GET /api/tenants/{id}/log-level`

테넌트에 저장된 MPL(Message Processing Log) 로그 레벨 설정을 조회합니다.

Response (`TenantLogLevelResponse`)
```json
{ "tenantId": 1, "logLevel": "DEBUG" }
```

Error: 저장된 설정이 없으면 `404 Not Found`

### 테넌트 로그 레벨 설정 (전체 아티팩트 일괄 적용)
`PUT /api/tenants/{id}/log-level`

지정한 로그 레벨을 DB에 저장(upsert)하고, 해당 테넌트에 배포된(`STARTED`) 아티팩트 전체에 즉시 반영합니다. 이후 10분마다 스케줄러(`app.tenant.log-level-cron`)가 저장된 값을 배포된 아티팩트 전체에 재적용(drift correction)합니다. 개별 아티팩트 적용이 실패해도 나머지 아티팩트 처리는 계속됩니다.

Request Body (`TenantLogLevelRequest`)
```json
{ "logLevel": "NONE | INFO | ERROR | DEBUG | TRACE" }
```

Response: `TenantLogLevelResponse` (위와 동일 형태)

---

## 4. Integration Package API

`IntegrationPackageController` — `/api/tenants/{tenantId}/packages`

패키지는 [테넌트 생성](#3-tenant-api) 시 자동으로 SAP에서 동기화되어 DB에 적재되므로, 별도의 동기화·삭제 API는 제공하지 않습니다. 목록 조회만 가능합니다.

### 패키지 목록 조회
`GET /api/tenants/{tenantId}/packages`

Response (`IntegrationPackageResponse[]`)
```json
[
  { "id": 1, "tenantId": 1, "sapPackageId": "string", "name": "string" }
]
```

---

## 5. Artifact API

`ArtifactController` — `/api/packages/{packageId}/artifacts`

### 아티팩트 동기화 (SAP에서 가져오기)
`POST /api/packages/{packageId}/artifacts/sync`

Response (`ArtifactResponse[]`)
```json
[
  {
    "id": 1,
    "integrationPackageId": 1,
    "sapArtifactId": "string",
    "name": "string",
    "version": "string",
    "type": "IFLOW"
  }
]
```

### 아티팩트 목록 조회
`GET /api/packages/{packageId}/artifacts`

Response: `ArtifactResponse[]`

### 아티팩트 삭제
`DELETE /api/packages/{packageId}/artifacts/{id}`

Response: 없음

---

## 6. Rule API

`RuleController` — `/api/rules`

개별 규칙(Rule) CRUD. `target`/`params`는 규칙 타입에 따라 자유 형식(JSON 객체)입니다.

### 규칙 생성
`POST /api/rules`

Request Body (`RuleCreateRequest`)
```json
{
  "ruleKey": "string",
  "isGlobal": true,
  "customProjectId": null,
  "type": "NAMING_CONVENTION",
  "severity": "FAIL",
  "target": {},
  "params": {},
  "message": "string",
  "enabled": true
}
```
- `customProjectId`: `isGlobal`이 `false`일 때 프로젝트 전용 규칙으로 연결할 프로젝트 ID.

Response (`RuleResponse`)
```json
{
  "id": 1,
  "ruleKey": "string",
  "isGlobal": true,
  "customProjectId": null,
  "type": "NAMING_CONVENTION",
  "severity": "FAIL",
  "target": {},
  "params": {},
  "message": "string",
  "enabled": true
}
```

### 규칙 단건 조회
`GET /api/rules/{id}`

Response: `RuleResponse`

### 규칙 수정
`PUT /api/rules/{id}`

Request Body (`RuleUpdateRequest`) — `ruleKey`, `type`, `isGlobal` 등은 수정 불가
```json
{
  "severity": "WARN",
  "target": {},
  "params": {},
  "message": "string",
  "enabled": true
}
```

Response: `RuleResponse`

### 규칙 삭제
`DELETE /api/rules/{id}`

Response: 없음

---

## 7. CheckRun API

`CheckRunController` — `/api/checkruns`

규칙 검사 실행 및 결과 조회.

### 단건 아티팩트 검사 실행
`POST /api/checkruns`

Request Body (`CheckRunRequest`)
```json
{ "projectId": 1, "artifactId": 1 }
```

Response (`CheckRunResponse`)
```json
{
  "id": 1,
  "projectId": 1,
  "status": "COMPLETED",
  "summary": {},
  "findings": [
    {
      "id": 1,
      "artifactId": 1,
      "ruleId": 1,
      "ruleKey": "string",
      "severity": "FAIL",
      "location": "string",
      "message": "string"
    }
  ]
}
```

### 패키지 전체 일괄 검사 실행
`POST /api/checkruns/batch`

Request Body (`CheckRunBatchRequest`)
```json
{ "projectId": 1, "integrationPackageId": 1 }
```

Response: `CheckRunResponse` (위와 동일 형태)

### 검사 실행 단건 조회
`GET /api/checkruns/{id}`

Response: `CheckRunResponse`

### 프로젝트별 검사 실행 목록 조회
`GET /api/checkruns?projectId={projectId}`

- `projectId`: 필수

Response: `CheckRunResponse[]`

---

## 8. Finding API

`FindingController` — `/api/findings`

검사에서 발견된 이슈(Finding)를 조건으로 검색.

### Finding 검색
`GET /api/findings?checkRunId={id}&severity={severity}&ruleId={id}&artifactId={id}`

- 모든 파라미터 선택. 지정한 조건만 AND로 필터링됩니다.

| 파라미터 | 타입 | 필수 |
|---|---|---|
| `checkRunId` | Long | 선택 |
| `severity` | `Severity` (`FAIL`/`WARN`/`INFO`) | 선택 |
| `ruleId` | Long | 선택 |
| `artifactId` | Long | 선택 |

Response (`FindingResponse[]`)
```json
[
  {
    "id": 1,
    "artifactId": 1,
    "ruleId": 1,
    "ruleKey": "string",
    "severity": "FAIL",
    "location": "string",
    "message": "string"
  }
]
```

---

## 9. Parser API (수동 검증용, 임시)

`ParserController` — SAP Integration Suite Connector 연동이 완성되기 전까지, ZIP 아티팩트를 직접 업로드해 Parser 출력을 확인하기 위한 임시 엔드포인트입니다. **Connector 완성 후 제거되거나 변경될 수 있으므로 프론트엔드 정식 연동 대상에서는 제외를 권장합니다.**

### 업로드한 아티팩트 파싱
`POST /api/parser/test`

Content-Type: `multipart/form-data`

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `file` | file | iFlow 등 아티팩트 ZIP 파일 |

Response (`ParsedModel`) — 개요만 표기 (세부 구조는 `parser/model` 패키지 참고)
```json
{
  "schemaVersion": 1,
  "artifact": { "...": "ArtifactInfo" },
  "iflow": { "...": "IflowModel (프로세스/이벤트/게이트웨이/스텝/시퀀스플로우 등)" },
  "parameters": [ { "...": "Parameter" } ],
  "mappings": [ { "...": "MappingArtifact" } ],
  "schemas": [ { "...": "SchemaArtifact" } ],
  "scripts": [ { "...": "ScriptArtifact" } ]
}
```

Error: 파싱 실패 시 `422 Unprocessable Entity` + `ErrorResponse`

---

## 10. Reprocess API

`MessageReprocessController` — `/api/reprocess`

SAP IS 메시지 재처리(DataStore/JMS), 메시지 바디 조회, 실패 로그(MPL) 조회 및 재실행 히스토리 관리.

### 아티팩트 재처리 지원 유형 조회
`GET /api/reprocess/artifacts/{artifactId}/support-type`

Response: `ReprocessSupportType` (`DATASTORE_ONLY` | `JMS_ONLY` | `BOTH` | `NONE`)

---

### MPL 실패 로그 목록 조회
`GET /api/reprocess/mpl-failures?tenantId={tenantId}&artifactId={artifactId}&top={top}`

- `tenantId`: 필수 (Long)
- `artifactId`: 선택 (String, SAP Artifact ID 또는 DB ID)
- `top`: 선택 (기본값: 20)

Response (`MplFailureResponse[]`):
```json
[
  {
    "messageGuid": "string",
    "correlationId": "string",
    "status": "FAILED | ESCALATED | CANCELLED",
    "artifactId": "string",
    "artifactName": "string",
    "logStart": "2026-08-24T10:00:00",
    "logEnd": "2026-08-24T10:01:00",
    "storageName": "string",
    "storageType": "DATASTORE | JMS | UNKNOWN",
    "expirationStatus": "NORMAL | WARNING_EXPIRING_SOON | EXPIRED",
    "daysLeft": 28,
    "errorDetail": "string"
  }
]
```

---

### 메시지 바디 조회
`GET /api/reprocess/messages/{messageId}/body?tenantId={tenantId}&artifactId={artifactId}&storageType={storageType}&storageName={storageName}`

- `tenantId`: 필수 (Long)
- `artifactId`: 필수 (Long)
- `storageType`: 필수 (`DATASTORE` | `JMS`)
- `storageName`: 선택 (String)

Response (`MessageBodyResponse`):
```json
{
  "messageId": "string",
  "storageType": "DATASTORE | JMS",
  "storageName": "string",
  "messageBody": "string",
  "expireDays": 30,
  "daysLeft": 28,
  "isExpired": false,
  "fetchedAt": "2026-08-24T10:00:00",
  "deepLinkUrl": "string"
}
```

---

### 메시지 재처리 실행
`POST /api/reprocess/execute`

Request Body (`MessageReprocessRequest`):
```json
{
  "tenantId": 1,
  "artifactId": "string (sapArtifactId)",
  "messageId": "string",
  "storageType": "DATASTORE | JMS",
  "storageName": "string",
  "reprocessedBy": "ADMIN",
  "payload": "string (선택: 미입력 시 DataStore에서 원본 바이너리 바디 자동 추출)",
  "endpointUrl": "string (선택: 미입력 시 SAP ServiceEndpoints 또는 기본 런타임 URL 자동 탐색)"
}
```

Response (`MessageReprocessResult`):
```json
{
  "historyId": 1,
  "messageId": "string",
  "success": true,
  "statusMessage": "string",
  "storageType": "DATASTORE | JMS",
  "storageName": "string",
  "reprocessedAt": "2026-08-24T10:00:00",
  "deepLinkUrl": "string",
  "endpointUrl": "string",
  "httpStatusCode": 200
}
```

---

### 재처리 히스토리 목록 조회
`GET /api/reprocess/histories?tenantId={tenantId}&artifactId={artifactId}&messageId={messageId}&status={status}`

- 모든 파라미터 선택적 (지정된 조건에 대해 AND 검색 및 최신순 정렬)
- `status`: `SUCCESS` | `FAILED` | `PENDING`

Response (`ReprocessHistoryResponse[]`):
```json
[
  {
    "id": 1,
    "tenantId": 1,
    "tenantName": "string",
    "artifactId": "string",
    "artifactName": "string",
    "messageId": "string",
    "storageType": "DATASTORE | JMS",
    "storageName": "string",
    "status": "SUCCESS | FAILED | PENDING",
    "statusMessage": "string",
    "reprocessedAt": "2026-08-24T10:00:00",
    "reprocessedBy": "ADMIN",
    "deepLinkUrl": "string",
    "endpointUrl": "string",
    "httpStatusCode": 200
  }
]
```

---

### 재처리 히스토리 단건 조회
`GET /api/reprocess/histories/{id}`

Response (`ReprocessHistoryResponse`):
```json
{
  "id": 1,
  "tenantId": 1,
  "tenantName": "string",
  "artifactId": "string",
  "artifactName": "string",
  "messageId": "string",
  "storageType": "DATASTORE | JMS",
  "storageName": "string",
  "status": "SUCCESS",
  "statusMessage": "string",
  "reprocessedAt": "2026-08-24T10:00:00",
  "reprocessedBy": "ADMIN",
  "deepLinkUrl": "string",
  "endpointUrl": "string",
  "httpStatusCode": 200
}
```

---

### 재처리 히스토리 삭제
`DELETE /api/reprocess/histories/{id}`

Response: 없음 (204 No Content)

---

### 저장소 매핑 목록 조회
`GET /api/reprocess/storage-mappings?tenantId={tenantId}&artifactId={artifactId}`

Response (`StorageMappingDto[]`):
```json
[
  {
    "tenantId": 1,
    "artifactId": 1,
    "storageType": "DATASTORE | JMS",
    "storageName": "string",
    "expireDays": 30,
    "confidenceLevel": "AUTO_PARSED | MANUAL_INPUT",
    "updatedAt": "2026-08-24T10:00:00"
  }
]
```

---

### 저장소 매핑 수동 저장/수정
`PUT /api/reprocess/storage-mappings`

Request Body (`StorageMappingDto`):
```json
{
  "tenantId": 1,
  "artifactId": 1,
  "storageType": "DATASTORE | JMS",
  "storageName": "string",
  "expireDays": 30
}
```

Response: `StorageMappingDto`

---

### 저장소 매핑 삭제
`DELETE /api/reprocess/storage-mappings?tenantId={tenantId}&artifactId={artifactId}`

Response: 없음 (204 No Content)

---

## 11. Notification API

`TenantNotificationController` — `/api/tenants/{tenantId}/notifications`

테넌트별 SAP IS 실패 메시지 이메일 리포팅 설정, 테스트 발송 및 즉시 리포트 발송 관리.

### 테넌트 알림 설정 조회
`GET /api/tenants/{tenantId}/notifications`

Response (`TenantNotificationConfigResponse`):
```json
{
  "id": 1,
  "tenantId": 1,
  "tenantName": "PROD_CF_TENANT",
  "isEnabled": true,
  "recipients": "admin@company.com, ops@company.com",
  "lastNotifiedAt": "2026-08-27T10:00:00"
}
```

### 테넌트 알림 설정 수정
`PUT /api/tenants/{tenantId}/notifications`

Request Body (`TenantNotificationConfigRequest`):
```json
{
  "isEnabled": true,
  "recipients": "admin@company.com, ops@company.com"
}
```

Response: `TenantNotificationConfigResponse` (위와 동일 형태)

### 테스트 이메일 발송
`POST /api/tenants/{tenantId}/notifications/test-mail`

Request Body (`TestEmailRequest`):
```json
{
  "targetEmail": "dev@company.com"
}
```

Response: 없음 (200 OK)

### 실패 리포트 즉시 발송 (수동 트리거)
`POST /api/tenants/{tenantId}/notifications/send-report?force={force}`

- `force`: 선택 (기본값 `false`). `true` 지정 시 신규 에러 발생 여부와 무관하게 현재 감지된 실패 목록 전체를 강제 발송.

Response (`NotificationHistoryResponse`):
```json
{
  "id": 1,
  "tenantId": 1,
  "tenantName": "PROD_CF_TENANT",
  "sentAt": "2026-08-27T10:00:00",
  "recipientCount": 2,
  "failureCount": 5,
  "status": "SUCCESS | FAILED",
  "subject": "[iFlow Sentinel] [경고] PROD_CF_TENANT 테넌트 실패 메시지 알림 (5건)",
  "errorMessage": null
}
```
- 신규 실패 건이 없어 발송되지 않은 경우: `204 No Content` 반환

### 알림 발송 히스토리 목록 조회
`GET /api/tenants/{tenantId}/notifications/histories`

Response (`NotificationHistoryResponse[]`):
```json
[
  {
    "id": 1,
    "tenantId": 1,
    "tenantName": "PROD_CF_TENANT",
    "sentAt": "2026-08-27T10:00:00",
    "recipientCount": 2,
    "failureCount": 5,
    "status": "SUCCESS",
    "subject": "[iFlow Sentinel] [경고] PROD_CF_TENANT 테넌트 실패 메시지 알림 (5건)",
    "errorMessage": null
  }
]
```

