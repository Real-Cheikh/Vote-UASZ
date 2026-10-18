
# Étape 1 : compiler le backend avec Maven et Java 21
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

# Étape 2 : image légère pour exécuter l'application
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S uasz && adduser -S voteuasz -G uasz \
    && mkdir -p /app/uploads \
    && chown -R voteuasz:uasz /app

COPY --from=build /app/target/*.jar app.jar

USER voteuasz

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
