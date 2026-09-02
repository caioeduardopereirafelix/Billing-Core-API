
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests \
 && cp target/*.jar target/app.jar

FROM eclipse-temurin:21-jre AS layers
WORKDIR /layers
COPY --from=build /workspace/target/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system --gid 1001 spring \
 && useradd  --system --uid 1001 --gid spring spring

COPY --from=layers --chown=spring:spring /layers/extracted/dependencies/ ./
COPY --from=layers --chown=spring:spring /layers/extracted/spring-boot-loader/ ./
COPY --from=layers --chown=spring:spring /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers --chown=spring:spring /layers/extracted/application/ ./

USER spring:spring

EXPOSE 8080


ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"


ENTRYPOINT ["java", "-jar", "app.jar"]
