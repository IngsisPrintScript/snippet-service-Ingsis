# syntax=docker/dockerfile:1

# --- Stage 1: build the application ---
FROM gradle:8.4-jdk21-alpine AS builder

ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /home/gradle/project

COPY . .

RUN mkdir -p /home/gradle/.gradle && \
    echo "gpr.user=${GITHUB_USER}" >> /home/gradle/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> /home/gradle/.gradle/gradle.properties

RUN chmod +x gradlew

RUN ./gradlew --no-daemon clean bootJar unzipNewRelic

# --- Stage 2: run the application ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
COPY --from=builder /home/gradle/project/build/newrelic/newrelic.jar /app/newrelic.jar
COPY --from=builder /home/gradle/project/build/newrelic/newrelic.yml /app/newrelic.yml

ENV JAVA_OPTS=""

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -javaagent:/app/newrelic.jar $JAVA_OPTS -jar app.jar"]
