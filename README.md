# iFlow Sentinel Backend

SAP Integration Suite(Cloud Integration)의 iFlow 아티팩트를 수집·파싱하고, 사내 규칙(Rule)에 따라 정적 점검을 수행하는 Java / Spring Boot 백엔드입니다.

## 기술 스택

- Java 21, Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA, Spring Security (JWT 인증)
- MySQL (운영/개발), H2 (테스트)
- Apache POI (Excel export), Spring Mail (실패 알림 메일)
- Gradle (Wrapper 포함)

## 프로젝트 구조

계층형 아키텍처를 따르며, 도메인별로 패키지를 분리합니다.

```
com.onetuks.iflow_sentinel
 ├─ auth/              # 로그인, JWT 발급/검증, 관리자 계정 초기화
 ├─ config/            # Security, Web, Connector, 로깅 필터 설정
 ├─ connector/         # SAP OData 연동 (테넌트/패키지/아티팩트 동기화)
 │   ├─ component/     # OAuth2 토큰 발급, SAP OData 클라이언트, 패키지 zip 파서
 │   ├─ controller/    # 테넌트/프로젝트/패키지/아티팩트 REST API
 │   ├─ domain/        # 테넌트, 프로젝트, 아티팩트 엔티티
 │   ├─ dto/           # 요청/응답 DTO
 │   ├─ scheduler/     # 주기적 동기화 스케줄러
 │   └─ service/       # 비즈니스 로직
 ├─ notification/      # 테넌트 실패 리포트 이메일 발송
 ├─ parser/            # iflw/zip/wsdl/mapping 등 SAP 아티팩트 파서
 ├─ report/            # 점검 실행(CheckRun) 및 결과(Finding) 관리
 ├─ reprocess/         # 실패 메시지 재처리 (SAP MPL 연동)
 ├─ rule/              # 점검 규칙(Rule) CRUD
 └─ ruleengine/        # 파싱된 모델에 규칙을 적용하는 평가 엔진
```

각 도메인은 `Controller → Service → Repository` 단방향 흐름을 유지합니다. 상세 컨벤션은 [AGENT.md](AGENT.md)를 참고하세요.

## 사전 준비

- JDK 21
- MySQL (로컬 실행 시 `compose.yaml`로 대체 가능)

## 로컬 실행

### 1. 데이터베이스 실행

```bash
docker compose up -d
```

`compose.yaml` 기준으로 `iflow_sentinel` DB, `iflow_user` 계정이 자동 생성됩니다 (포트 3306).

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 `8888`이며, 주요 설정은 [src/main/resources/application.yaml](src/main/resources/application.yaml)에서 확인할 수 있습니다.

| 환경변수 | 설명 | 기본값 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | DB 접속 URL | `jdbc:mysql://localhost:3306/iflow_sentinel` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | DB 계정 | `iflow_user` / `iflow_password` |
| `CREDENTIAL_ENCRYPTION_KEY` | 테넌트 인증정보 암호화 키 | 로컬 개발용 기본값 있음 |
| `JWT_SECRET` | JWT 서명 키 | 로컬 개발용 기본값 있음 |
| `SPRING_MAIL_*` | 실패 리포트 발송용 SMTP 설정 | 미설정 시 빈 값 |
| `APP_DASHBOARD_BASE_URL` | 알림 메일에 삽입할 프론트엔드 URL | `http://localhost:3000` |

운영 환경에서는 `CREDENTIAL_ENCRYPTION_KEY`, `JWT_SECRET`을 반드시 별도 값으로 지정해야 합니다.

### 3. 테스트 실행

```bash
./gradlew test
```

## Docker 빌드

```bash
docker build -t iflow-sentinel-backend .
```

멀티 스테이지 빌드로 `eclipse-temurin:21-jre-alpine` 기반 이미지를 생성하며, 컨테이너는 `8888` 포트를 노출합니다.

## API 문서

전체 엔드포인트 명세는 [docs/API.md](docs/API.md)를 참고하세요. 메시지 재처리 관련 상세는 [references/메시지_재처리_API.md](references/메시지_재처리_API.md)에 별도 정리되어 있습니다.

## 관련 문서

- [AGENT.md](AGENT.md) — 계층 분리, SOLID, 설계서 반영 원칙 등 개발 컨벤션
- [HELP.md](HELP.md) — Spring Boot Gradle 참고 링크
