# VoteUASZ — Plateforme Officielle de Vote Électronique Sécurisé
**Université Assane Seck de Ziguinchor (UASZ) — Sénégal**

---

## 1. Présentation du Projet

**VoteUASZ** est une solution logicielle universitaire d'élection en ligne conçue pour moderniser, sécuriser et fiabiliser les scrutins de l'**Université Assane Seck de Ziguinchor (UASZ)**.

L'application permet d'organiser et de gérer de bout en bout l'ensemble des élections universitaires :
- **Élections des délégués étudiants** (par UFR, département et promotion).
- **Élections des directeurs d'UFR et chefs de départements**.
- **Élections des vice-recteurs et représentants du personnel**.
- **Scrutins institutionnels configurables**.

### Principes Fondamentaux Respectés
- **Unicité stricte du vote** : double vote rendu techniquement impossible au niveau transactionnel.
- **Secret absolu du bulletin** : anonymat garanti par découplage architectural strict (aucune référence à l'identité de l'électeur dans l'urne).
- **Authentification forte** : mots de passe chiffrés en **Argon2id** et validation à deux facteurs par **code OTP (validité 5 minutes)**.
- **Traçabilité & Auditabilité** : journal d'audit immuable avec chaînage cryptographique d'intégrité (Hash-chaining SHA-256).
- **Contrôle dynamique de l'éligibilité** : vérification par règles de collèges côté serveur.
- **Certification des résultats** : génération automatique d'un **Procès-Verbal officiel au format PDF** scellé par une empreinte SHA-256.
- **Diffusion en direct** : transmission temps réel des résultats via **WebSocket STOMP**.

> [!NOTE]
> Il s'agit d'un projet académique rigoureux développé dans l'environnement Google Antigravity. Bien qu'intégrant des protocoles cryptographiques stricts, le système ne doit pas être qualifié de solution de vote étatique sans un audit indépendant préalable.

---

## 2. Architecture Technique (Monorepo)

```
VoteUASZ/
├── backend/                  # API REST Spring Boot 3.3.5 (Java 21 LTS)
│   ├── src/main/java/sn/uasz/voteuasz/
│   │   ├── auth/             # Connexion, JWT, renouvellement
│   │   ├── users/            # Utilisateurs et profils UASZ
│   │   ├── roles/            # Rôles (Super Admin, Commission, Candidat, Électeur)
│   │   ├── colleges/         # Collèges électoraux (UFR, départements)
│   │   ├── elections/        # Scrutins, cycle de vie et règles
│   │   ├── candidacies/      # Dépôt, pièces justificatives, validation
│   │   ├── campaigns/        # Programmes électoraux et médias
│   │   ├── eligibility/      # Registre et contrôle d'éligibilité
│   │   ├── otp/              # Codes OTP temporaires à usage unique
│   │   ├── voting/           # Moteur de vote anonymisé (VoteCredential, Ballots)
│   │   ├── results/          # Dépouillement et diffusion WebSocket
│   │   ├── reports/          # Procès-verbaux PDF certifiés (OpenPDF)
│   │   ├── complaints/       # Contentieux et réclamations
│   │   ├── audit/            # Journal d'audit chaîné
│   │   ├── config/           # OpenAPI Swagger, WebSocket STOMP
│   │   └── security/         # Filtre JWT, Argon2id, bootstrapping admin
│   ├── src/main/resources/db/migration/ # Migrations versionnées Flyway
│   ├── pom.xml               # Configuration Maven
│   └── Dockerfile            # Image Docker multi-stage Java 21
├── frontend/                 # Application Web Responsive Angular & PWA
├── docs/                     # Spécifications, sécurité, déploiement & UML
│   ├── architecture.md
│   ├── security.md
│   ├── deployment.md
│   └── uml/
├── compose.yaml              # Orchestration Docker Compose locale
├── .env.example              # Gabarit des variables d'environnement
└── README.md
```

---

## 3. Prérequis et Outils Requis

- **Java JDK 21 LTS** (`Eclipse Temurin` ou `Oracle JDK 21`).
- **Apache Maven 3.9+** (ou utilisation directe du wrapper `./mvnw` inclus).
- **Node.js v20+** ou **v24 LTS** avec **npm**.
- **PostgreSQL 16** (ou conteneur Docker).
- **Docker & Docker Compose** (recommandé pour l'environnement complet).

---

## 4. Démarrage Rapide

### Option A : Avec Docker Compose (Recommandé)

```powershell
# Démarrer PostgreSQL, le Backend et le Frontend en une seule commande
docker compose up -d --build
```
- Frontend : `http://localhost:4200`
- Backend API : `http://localhost:8080/api/v1`
- Swagger UI : `http://localhost:8080/api/v1/swagger-ui.html`

### Option B : Exécution Manuelle du Backend

```powershell
cd backend
# Exécuter l'application avec le profil de développement
.\mvnw.cmd spring-boot:run
```

---

## 5. Compte Administrateur Initial (Bootstrap)

Au premier démarrage, le système génère automatiquement le premier compte Super-Administrateur conformément aux variables d'environnement configurées :

- **Email** : `admin.electoral@uasz.sn`
- **Mot de passe** : `AdminUasz2026!Securise` (haché en Argon2id)
- **Rôles** : `ROLE_SUPER_ADMIN_ELECTORAL`, `ROLE_COMMISSION_ELECTORALE`

> [!WARNING]
> En environnement de production, changez impérativement la variable `INITIAL_ADMIN_PASSWORD` dans votre fichier `.env` ou les paramètres de déploiement cloud avant le premier lancement !

---

## 6. Documentation des Endpoints REST (OpenAPI 3 / Swagger)

L'API est entièrement interactive et testable via l'interface Swagger UI :
- **URL locale** : `http://localhost:8080/api/v1/swagger-ui.html`
- **Spécification JSON OpenAPI** : `http://localhost:8080/api/v1/v3/api-docs`

---

## 7. Exécution des Tests Automatisés

Le projet comprend une suite complète de 21 tests d'intégration et unitaires vérifiant l'ensemble des scénarios critiques :

```powershell
cd backend
.\mvnw.cmd test
```

### Scénarios Validés par les Tests
1. Connexion avec email et mot de passe corrects / incorrects.
2. Hachage cryptographique **Argon2id**.
3. Contrôle des autorisations et rôles de sécurité (`SUPER_ADMIN_ELECTORAL`, `COMMISSION_ELECTORALE`, `ELECTEUR`).
4. Rejet d'accès administratif aux électeurs non autorisés.
5. Contrôle dynamique de l'éligibilité selon le collège de l'électeur.
6. Rejet de vote sur scrutin non ouvert ou déjà clôturé.
7. Génération et vérification d'OTP légitime.
8. **Expiration stricte de l'OTP après 5 minutes**.
9. **Rejet strict des codes OTP réutilisés (usage unique)**.
10. **Blocage après 3 tentatives infructueuses (anti-bruteforce)**.
11. **Anti-double vote atomique au niveau du registre électoral**.
12. **Résistance aux requêtes concurrentes (10 threads simultanés -> 1 seul vote accepté)**.
13. Secret absolu du bulletin (aucune donnée personnelle dans la table des bulletins).
14. Reçu de vote anonyme avec empreinte cryptographique SHA-256.
15. Dépouillement exact et intégrité des totaux de suffrages.
16. Interdiction d'accès aux résultats avant publication officielle.
17. Génération et signature du **Procès-Verbal officiel PDF**.
18. Absence de fuite de données sensibles dans les journaux d'audit.
19. Blocage des téléversements de fichiers malveillants (`.exe`, `.sh`).
20. Gestion des contestations et décisions motivées.
