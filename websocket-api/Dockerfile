FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy and install parent pom first
COPY pom.xml ./
RUN mvn -N install

# Copy and build shared-domain
COPY shared-domain ./shared-domain
RUN mvn -f shared-domain/pom.xml clean install -DskipTests

# Copy websocket-api pom and download dependencies
COPY websocket-api/pom.xml ./websocket-api/
RUN mvn -f websocket-api/pom.xml dependency:go-offline -DskipTests || true

# Copy source and build
COPY websocket-api/src ./websocket-api/src
RUN mvn -f websocket-api/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built artifact
COPY --from=build /app/websocket-api/target/quarkus-app/ ./

# Expose port
EXPOSE 8080

# Environment variables
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV BROADCAST_SYMBOLS="AAPL,GOOGL,MSFT,TSLA,AMZN"
ENV BROADCAST_INTERVAL="1s"

# Run the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
