# ==========================================
# 1단계: 빌드 스테이지 (Build Stage)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle 래퍼 및 빌드 설정 파일 복사 (의존성 캐싱 목적)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./

# 윈도우 환경 줄바꿈(CRLF) 이슈 방지 및 실행 권한 부여
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# 의존성 사전 다운로드 (소스 변경 시 빌드 속도 최적화)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 실행 가능한 bootJar 빌드 (테스트 제외)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ==========================================
# 2단계: 실행 스테이지 (Runtime Stage)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안을 위한 비루트(Non-root) 사용자 생성 및 전환
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 시간대 설정 (KST 기준)
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 빌드 스테이지에서 생성된 jar 파일 복사
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

USER appuser

# 백엔드 서버 포트 노출 (application.yaml 기준 8888)
EXPOSE 8888

# JVM 최적화 옵션 및 실행 환경변수 지원
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
