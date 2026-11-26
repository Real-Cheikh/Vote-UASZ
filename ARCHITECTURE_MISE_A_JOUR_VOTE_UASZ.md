# ARCHITECTURE MISE À JOUR DU PROJET VOTE UASZ
**Date :** 26 Novembre 2026  
**Projet :** Vote UASZ  

---

## 1. VUE D'ENSEMBLE TECHNIQUE

```
+---------------------------------------------------------------------------------------+
|                                    VOTE UASZ ARCHITECTURE                             |
+---------------------------------------------------------------------------------------+
|  CLIENT (Navigateur / Application Web Responsive)                                     |
|  - Angular 21.2.0 Standalone Components & Signals réactifs                            |
|  - Clients HTTP avec intercepteur JWT & reconnexion STOMP WebSocket                   |
|  - Routage protégé par authGuard et commissionGuard                                   |
+-------------------------------------------+-------------------------------------------+
                                            | HTTPS / WSS
                                            v
+---------------------------------------------------------------------------------------+
|  SERVEUR D'APPLICATION (Spring Boot 3.3.5 / Java 21)                                  |
|  - Filtre Stateless JwtAuthenticationFilter + Chiffrement Argon2id                     |
|  - Contrôleurs REST documentés OpenAPI 3                                              |
|  - Services Métier Transactionnels & Thread-Safe (Audit chaîné synchronisé)            |
|  - Urne électronique découplée en deux étapes (Token éphémère 15 min)                 |
|  - Générateur de Procès-Verbaux PDF certifiés OpenPDF 2.0.3                            |
|  - Broker de messages Spring STOMP (/topic/results/*)                                 |
+-------------------------------------------+-------------------------------------------+
                                            | JDBC Pool HikariCP
                                            v
+---------------------------------------------------------------------------------------+
|  PERSISTENCE & STOCKAGE                                                               |
|  - PostgreSQL 15+ (Profils dev/prod via Supabase) / H2 (Profil test)                  |
|  - Schéma versionné par Flyway (V1, V2, V3)                                           |
|  - Stockage local sécurisé des justificatifs & rapports avec anti-path traversal      |
+---------------------------------------------------------------------------------------+
```

---

## 2. MODIFICATIONS ARCHITECTURALES ET FLUX APPORTÉS

1. **Règles d'émission des rapports PDF :**
   Le service de génération de PV (`PdfReportGeneratorService`) est désormais couplé au contrôle de publication de `ResultsService`. Toute tentative de génération antérieure à l'état `RESULTS_PUBLISHED` est bloquée au niveau applicatif.

2. **Module de Campagne enrichi :**
   Intégration d'un flux d'écriture sécurisé `POST /campaigns/my/{candidacyId}` garantissant le principe de moindre privilège et l'isolation des données entre candidats concurrents.

3. **Protection contre la concurrence sur le chaînage cryptographique :**
   Synchronisation de la méthode d'audit garantissant l'ordonnancement séquentiel immuable de chaque événement électoral dans la chaîne SHA-256.

4. **Navigation Frontend :**
   Enrichissement de la barre de navigation avec un accès permanent à l'espace de recours et de contentieux (`/complaints`) pour l'électeur connecté.
