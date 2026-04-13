FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -N install

COPY shared-domain ./shared-domain
RUN mvn -f shared-domain/pom.xml clean install -DskipTests

COPY ai-decision-service/pom.xml ./ai-decision-service/
RUN mvn -f ai-decision-service/pom.xml dependency:go-offline -DskipTests || true

COPY ai-decision-service/src ./ai-decision-service/src
RUN mvn -f ai-decision-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/ai-decision-service/target/quarkus-app/ ./

EXPOSE 8084

ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV OPENAI_API_KEY=""
ENV OPENAI_MODEL="gpt-4o-mini"
ENV DECISION_CONFIDENCE_FLOOR="0.55"

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
