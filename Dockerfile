# syntax=docker/dockerfile:1

# =========================
# Stage 1: Build
# =========================
FROM gradle:8.4-jdk21-alpine AS builder

ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /home/gradle/project

# Copiamos todo el proyecto
COPY . .

# Credenciales para GitHub Packages (solo build)
RUN mkdir -p /home/gradle/.gradle && \
    echo "gpr.user=${GITHUB_USER}" > /home/gradle/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> /home/gradle/.gradle/gradle.properties

RUN chmod +x gradlew

# Build + descarga y unzip de New Relic
RUN ./gradlew --no-daemon clean bootJar unzipNewRelic -x test


# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# App
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# New Relic (path REAL luego del unzip)
RUN mkdir -p /app/newrelic
COPY --from=builder /home/gradle/project/build/newrelic/newrelic/newrelic.jar /app/newrelic/newrelic.jar
COPY --from=builder /home/gradle/project/build/newrelic/newrelic/newrelic.yml /app/newrelic/newrelic.yml

ENV JAVA_OPTS=""
ENV NEW_RELIC_LOG=stdout

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -javaagent:/app/newrelic/newrelic.jar $JAVA_OPTS -jar app.jar"]