# syntax=docker/dockerfile:1


FROM gradle:8.4-jdk21-alpine AS builder

ARG GITHUB_USER

COPY . .

RUN mkdir -p /home/gradle/.gradle && \
    echo "gpr.user=${GITHUB_USER}" > /home/gradle/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> /home/gradle/.gradle/gradle.properties

RUN chmod +x gradlew


RUN ./gradlew --no-daemon clean bootJar unzipNewRelic -x test

WORKDIR /home/gradle/project

# Copiar sólo los archivos necesarios para cachear dependencias
COPY build.gradle settings.gradle gradlew gradle /home/gradle/project/

# Descargar dependencias usando el wrapper
RUN chmod +x gradlew && ./gradlew --no-daemon assemble -x test || return 0

# Copiar el resto del código
COPY . /home/gradle/project

# Build real, generando el .jar usando el wrapper
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar



FROM eclipse-temurin:21-jre-alpine

WORKDIR /app


COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar


COPY --from=builder /home/gradle/project/build/ /tmp/build/

RUN find /tmp/build -name "newrelic.jar" -exec cp {} /app/newrelic.jar \; \
 && find /tmp/build -name "newrelic.yml" -exec cp {} /app/newrelic.yml \;

ENV JAVA_OPTS=""
EXPOSE 8081
