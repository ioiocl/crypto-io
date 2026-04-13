FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -N install

COPY shared-domain ./shared-domain
RUN mvn -f shared-domain/pom.xml clean install -DskipTests

COPY news-service/pom.xml ./news-service/
RUN mvn -f news-service/pom.xml dependency:go-offline -DskipTests || true

COPY news-service/src ./news-service/src
RUN mvn -f news-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/news-service/target/quarkus-app/ ./

EXPOSE 8083

ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV NEWS_SYMBOLS="BTC,ETH,BNB,SOL,XRP"
ENV NEWS_POLL_INTERVAL="5m"
ENV NEWS_API_KEY=""

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
