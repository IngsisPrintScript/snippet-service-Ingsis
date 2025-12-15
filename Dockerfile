# syntax=docker/dockerfile:1

FROM gradle:8.4-jdk21-alpine AS builder

ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /home/gradle/project

COPY . .

RUN mkdir -p /home/gradle/.gradle && \
    echo "gpr.user=${GITHUB_USER}" > /home/gradle/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> /home/gradle/.gradle/gradle.properties

RUN chmod +x gradlew

RUN ./gradlew --no-daemon clean bootJar unzipNewRelic -x test


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
COPY --from=builder /home/gradle/project/build/ /tmp/build/

RUN find /tmp/build -name "newrelic.jar" -exec cp {} /app/newrelic.jar \; \
 && find /tmp/build -name "newrelic.yml" -exec cp {} /app/newrelic.yml \;

ENV JAVA_OPTS=""
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -javaagent:/app/newrelic.jar $JAVA_OPTS -jar app.jar"]