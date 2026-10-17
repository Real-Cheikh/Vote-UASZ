# Étape 1 : Construction de l'application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copie des configurations Maven et du code source
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# Téléchargement des dépendances hors ligne
RUN ./mvnw dependency:go-offline -B

COPY src ./src
# Compilation du JAR exécutable en ignorant les tests pour accélérer le build Docker
RUN ./mvnw clean package -DskipTests -B

# Étape 2 : Image d'exécution légère et sécurisée
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -S uasz && adduser -S voteuasz -G uasz

# Répertoire pour les téléversements
RUN mkdir -p /app/uploads && chown -R voteuasz:uasz /app

USER voteuasz

COPY --from=builder /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
