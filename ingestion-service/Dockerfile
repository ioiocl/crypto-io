FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy and install parent pom first
COPY pom.xml ./
RUN mvn -N install

# Copy and build shared-domain
COPY shared-domain ./shared-domain
RUN mvn -f shared-domain/pom.xml clean install -DskipTests

# Copy ingestion-service pom and download dependencies
COPY ingestion-service/pom.xml ./ingestion-service/
RUN mvn -f ingestion-service/pom.xml dependency:go-offline -DskipTests || true

# Copy source and build
COPY ingestion-service/src ./ingestion-service/src
RUN mvn -f ingestion-service/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built artifact
COPY --from=build /app/ingestion-service/target/quarkus-app/ ./

# Expose port
EXPOSE 8081

# Environment variables
ENV POLYGON_API_KEY=""
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV POLYGON_SYMBOLS="AAPL,GOOGL,MSFT,TSLA,AMZN"

# Run the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
