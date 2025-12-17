FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy and install parent pom first
COPY pom.xml ./
RUN mvn -N install

# Copy and build shared-domain
COPY shared-domain ./shared-domain
RUN mvn -f shared-domain/pom.xml clean install -DskipTests

# Copy analytics-service pom and download dependencies
COPY analytics-service/pom.xml ./analytics-service/
RUN mvn -f analytics-service/pom.xml dependency:go-offline -DskipTests || true

# Copy source and build
COPY analytics-service/src ./analytics-service/src
RUN mvn -f analytics-service/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built artifact
COPY --from=build /app/analytics-service/target/quarkus-app/ ./

# Expose port
EXPOSE 8082

# Environment variables
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV ANALYTICS_SYMBOLS="AAPL,GOOGL,MSFT,TSLA,AMZN"
ENV SNAPSHOT_INTERVAL="5s"

# Run the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
