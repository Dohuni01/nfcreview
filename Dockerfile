# ---------- 1단계: 빌드 (JDK가 있는 이미지에서 jar를 만든다) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 빌드 설정을 먼저 복사해서 라이브러리 다운로드를 캐시해 둔다.
# 소스만 바뀐 빌드에서는 이 층을 재사용해서 훨씬 빨라진다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# 윈도우 줄바꿈(CRLF)이 섞여 들어와도 리눅스에서 실행되게 정리
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar

# ---------- 2단계: 실행 (JRE와 jar만 담은 가벼운 이미지) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root가 아닌 전용 사용자로 실행 (혹시 뚫려도 피해를 줄이기 위해)
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/build/libs/app.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
