# Étape 1 : compiler le backend avec Maven et Java 21
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src src

RUN mvn -B clean package -DskipTests

# Étape 2 : image d'exécution légère et sécurisée
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r uasz && useradd -r -g uasz voteuasz \
    && mkdir -p /app/uploads \
    && chown -R voteuasz:uasz /app

COPY --from=build /app/target/*.jar app.jar

USER voteuasz

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xss512k -XX:+ExitOnOutOfMemoryError -jar app.jar --server.port=${PORT:-8080}"]
