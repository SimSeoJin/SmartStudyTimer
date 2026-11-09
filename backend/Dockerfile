# 백엔드 이미지. 빌드 컨텍스트는 레포 루트 (docker-compose.yml 참고)

# 1) 빌드 스테이지
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 루트 settings.gradle은 Android 모듈(mobile:app)도 포함해서 Android SDK 없이는 실패하므로,
# 컨테이너 안에서는 백엔드만 포함하는 settings.gradle로 덮어씀.
# gradlew는 Windows에서 CRLF로 커밋돼 있어서 줄바꿈도 변환.
COPY gradlew build.gradle ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
 && printf "rootProject.name = 'SmartStudyTimer'\ninclude 'backend'\n" > settings.gradle

# 의존성을 먼저 받아서 소스만 바뀌었을 때는 캐시된 레이어 재사용
COPY backend/build.gradle backend/
RUN ./gradlew :backend:dependencies --no-daemon > /dev/null

COPY backend/src backend/src
RUN ./gradlew :backend:bootJar --no-daemon \
 && cp "$(ls backend/build/libs/*.jar | grep -v plain)" app.jar

# 2) 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /workspace/app.jar app.jar
USER app

# t3.small(2GB)에서 MySQL과 같이 돌리므로 힙 상한을 둠
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
