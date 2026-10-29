# Backend image: builds the jar, then runs it on a plain JRE as a non-root user.
# Build context is ./backend (deploy/docker-compose.yml sets it).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Dependencies first, so editing source does not download them again.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --console=plain dependencies > /dev/null

COPY src src
# Tests need Docker (Testcontainers) and run in CI; the image only needs the jar.
RUN ./gradlew --no-daemon --console=plain bootJar -x test \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)" /app.jar

FROM eclipse-temurin:21-jre
# Photos live on a volume, never in the image: a rebuilt container must find them again.
RUN useradd --system --uid 10001 --home-dir /app app \
    && mkdir -p /app /data/photos \
    && chown -R app:app /app /data
WORKDIR /app
COPY --from=build /app.jar /app/app.jar

ENV PHOTO_STORAGE_PATH=/data/photos \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
USER app
EXPOSE 10002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
