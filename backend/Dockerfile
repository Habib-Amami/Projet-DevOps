# syntax=docker/dockerfile:1

# =============================================================================
# Spring Boot (achat) - optimized multi-stage image
#
# Stage 1 (builder): compile the app with Maven and explode the fat JAR into
#                    Spring Boot layers, ordered from least- to most-frequently
#                    changed so Docker can reuse cache between builds.
# Stage 2 (runtime): minimal JRE, non-root user, only the extracted layers.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 - build
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 1. Maven wrapper only (rarely changes) - keeps the wrapper download cached.
COPY .mvn/ .mvn/
COPY mvnw ./
RUN chmod +x mvnw

# 2. POM only (changes less often than source) - caches the full dependency set.
COPY pom.xml ./
RUN ./mvnw -B -ntp -q dependency:go-offline

# 3. Application source + a full package build (tests run in CI, not here).
COPY src ./src
RUN ./mvnw -B -ntp clean package \
        -DskipTests \
        -Dasciidoctor.skip=true \
        -Dspring-boot.repackage.skip=false

# 4. Explode the fat JAR into layers (dependencies / loader / snapshot / app).
#    --launcher keeps the classic BOOT-INF layout so JarLauncher can run the
#    exploded layers (without it, Boot 4 emits a lib/ + thin-jar layout).
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# -----------------------------------------------------------------------------
# Stage 2 - runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="achat-backend" \
      org.opencontainers.image.description="Spring Boot backend for the achat application" \
      org.opencontainers.image.source="https://github.com/org/Projet-DevOps"

# Unprivileged user.
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Copy layers from least to most likely to change (best cache reuse).
COPY --from=builder --chown=app:app /build/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /build/extracted/application/ ./

# Modeled, container-aware JVM defaults; override at runtime with -e JAVA_OPTS=...
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8089

USER app
EXPOSE 8089

# Fail the container when the actuator health endpoint is not UP.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
