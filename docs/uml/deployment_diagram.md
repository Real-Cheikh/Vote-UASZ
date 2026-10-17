# Diagramme de Déploiement UML - VoteUASZ

```mermaid
flowchart TB
    subgraph Clients ["Clients Utilisateurs"]
        BrowserMobile["Navigateur Mobile (PWA Android / iOS)"]
        BrowserDesktop["Navigateur Desktop (Chrome / Firefox / Edge)"]
    end

    subgraph CDN ["Réseau Edge / CDN (Vercel ou Netlify)"]
        VercelEdge["Vercel Edge Network / Nginx Proxy"]
        AngularApp["Frontend Angular Build (HTML5, CSS, TS, PWA)"]
    end

    subgraph CloudPlatform ["Plateforme d'Exécution Backend (Render / Railway / VPS UASZ)"]
        subgraph DockerContainer ["Conteneur Docker VoteUASZ"]
            Temurin["Eclipse Temurin 21 JRE Alpine"]
            SpringBoot["Application VoteUASZ Spring Boot 3.3.5"]
            API_Engine["Moteur API REST & OpenAPI 3"]
            WS_Broker["Serveur WebSocket STOMP"]
            Sec_Engine["Moteur Sécurité JWT & Argon2id"]
        end
        VolUploads[("Volume Persistant : /app/uploads")]
    end

    subgraph ManagedDB ["Base de Données Managée Persistante"]
        PostgresInstance[("PostgreSQL 16 Engine")]
        FlywayEngine["Flyway Versioned Migrations"]
        VolData[("Storage Volume SSD chiffré")]
    end

    BrowserMobile -->|HTTPS / WSS| VercelEdge
    BrowserDesktop -->|HTTPS / WSS| VercelEdge
    VercelEdge --> AngularApp
    AngularApp -->|HTTPS / JSON API (Port 443/8080)| SpringBoot
    AngularApp -->|WSS / STOMP| WS_Broker

    SpringBoot --> API_Engine
    SpringBoot --> Sec_Engine
    SpringBoot --> VolUploads

    API_Engine -->|JDBC Pooling HikariCP (SSL Mode Required)| PostgresInstance
    PostgresInstance --> FlywayEngine
    PostgresInstance --> VolData
```
