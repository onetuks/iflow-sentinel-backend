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
  "clientSecret": "string"
}
```

Response (`TenantResponse`) — `clientSecret`은 응답에 포함되지 않음
```json
{
  "id": 1,
  "projectId": 1,
  "name": "string",
  "odataUrl": "string",
  "tokenUrl": "string",
  "platformType": "NEO",
  "authType": "OAUTH2_CLIENT_CREDENTIALS",
  "clientId": "string"
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
