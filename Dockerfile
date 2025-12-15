# --- Stage 1: build the application ---
FROM gradle:8.4-jdk21-alpine AS builder
# (usa la imagen oficial de Gradle con JDK 21)

WORKDIR /home/gradle/project

# Copiar sólo los archivos necesarios para cachear dependencias
COPY build.gradle settings.gradle gradlew gradle /home/gradle/project/

# Descargar dependencias usando el wrapper
RUN chmod +x gradlew && ./gradlew --no-daemon assemble -x test || return 0

# Copiar el resto del código
COPY . /home/gradle/project

# Build real, generando el .jar usando el wrapper
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

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
