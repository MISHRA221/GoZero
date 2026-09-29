# syntax=docker/dockerfile:1
# Build context is the repository root (see docker-compose.yml): the parent pom is needed for the reactor build.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY ingestion-service/pom.xml ingestion-service/
COPY sentiment-service/pom.xml sentiment-service/
COPY reconciliation-service/pom.xml reconciliation-service/
COPY dashboard-service/pom.xml dashboard-service/
# Cache dependencies in their own layer so source edits don't re-download the internet
RUN mvn -B -q -pl ingestion-service -am dependency:go-offline -DskipTests || true
COPY ingestion-service/src ingestion-service/src
RUN mvn -B -pl ingestion-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build /workspace/ingestion-service/target/ingestion-service.jar app.jar
USER 10001:10001
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
