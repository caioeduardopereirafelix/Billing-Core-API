# syntax=docker/dockerfile:1

# ---- build: Maven + JDK 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests \
 && cp target/*.jar target/app.jar

# ---- explode into Spring Boot layers ----
FROM eclipse-temurin:21-jre AS layers
WORKDIR /layers
COPY --from=build /workspace/target/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---- runtime: JRE 21, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 1001 spring \
 && useradd  --system --uid 1001 --gid spring spring

COPY --from=layers --chown=spring:spring /layers/extracted/dependencies/ ./
COPY --from=layers --chown=spring:spring /layers/extracted/spring-boot-loader/ ./
COPY --from=layers --chown=spring:spring /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers --chown=spring:spring /layers/extracted/application/ ./

USER spring:spring
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
