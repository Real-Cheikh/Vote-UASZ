# Architecture Globale de VoteUASZ

## 1. Présentation Générale

**VoteUASZ** est la plateforme officielle de gestion et de déroulement des élections universitaires pour l'**Université Assane Seck de Ziguinchor (UASZ)** au Sénégal.

L'application est structurée selon une architecture monorepo moderne séparant rigoureusement le frontend web réactif (Angular) et le backend d'API sécurisé (Spring Boot) adossé à un SGBD relationnel robuste (PostgreSQL).

```mermaid
graph TD
    User["Utilisateur / Électeur / Commission<br>(Navigateur Web / Mobile)"]
    
    subgraph Frontend ["Frontend Angular (PWA)"]
        UI["Composants UI (Angular Material / Tailwind)"]
        Router["Angular Router & Guards (Rôles & Auth)"]
        State["Services HTTP & Client WebSocket STOMP"]
    end
    
    subgraph Backend ["Backend Spring Boot 3.3 (Java 21 LTS)"]
        SecFilter["Spring Security (Filtre JWT Bearer & Argon2id)"]
        REST["Contrôleurs REST (/api/v1/...)"]
        WS["Courtier WebSocket STOMP (/ws, /topic/results)"]
        
        subgraph Services ["Services Métiers Découplés"]
            AuthServ["Auth & Gestion Utilisateurs"]
            ElecServ["Gestion Élections & Collèges"]
            EligServ["Registre d'Éligibilité (a_vote)"]
            OTPServ["Service OTP (Cryptographie & Expiration 5 min)"]
            VoteServ["Urne Électronique Anonymisée (Ballots)"]
            ResServ["Dépouillement & Certification"]
            AuditServ["Audit Immuable (Chaînage SHA-256)"]
        end
    end
    
    subgraph Database ["Base de Données PostgreSQL 16"]
        Flyway["Migrations Flyway"]
        TablesUser["Tables : users, roles, colleges, elections"]
        TablesElig["Table : eligibility_records (user_id, a_vote)"]
        TablesVote["Table : ballots (candidacy_id, hash - SANS user_id)"]
        TablesAudit["Table : audit_events"]
    end
    
    User --> UI
    UI --> Router
    Router --> State
    State -->|HTTPS / REST API| SecFilter
    State -->|WSS / STOMP| WS
    SecFilter --> REST
    REST --> Services
    Services --> Database
```

## 2. Découplage Strict et Secret du Scrutin

L'exigence fondamentale de VoteUASZ est d'empêcher tout double vote tout en rendant mathématiquement et structurellement impossible de relier l'identité d'un électeur à son vote :

1. **Phase 1 : Identification & Éligibilité**
   - L'électeur s'authentifie par email et mot de passe (haché en Argon2id).
   - Le système vérifie son éligibilité dynamique dans le scrutin selon son collège (`UFR`, `département`, `catégorie`).
   - Un code OTP aléatoire de 6 chiffres est généré, haché en SHA-256 et stocké pour une durée maximale de 5 minutes.

2. **Phase 2 : Consommation du Droit de Vote & Délivrance du Jeton Anonyme**
   - L'électeur saisit son code OTP.
   - Le service d'éligibilité met à jour de façon **atomique** le registre électoral (`UPDATE eligibility_records SET a_vote = true WHERE user_id = ? AND a_vote = false`). Si la requête ne modifie aucune ligne (tentative concurrente), le vote est rejeté.
   - Un **jeton d'autorisation de vote anonyme** (`VoteCredential`) à usage unique (valable 15 minutes) est généré cryptographiquement et remis au client.

3. **Phase 3 : Dépôt du Bulletin dans l'Urne (Anonymat Total)**
   - L'électeur transmet son bulletin accompagné de son jeton anonyme.
   - Le jeton est consommé de manière atomique (`utilise = true`).
   - Le bulletin est inséré dans la table `ballots`. **Cette table ne possède AUCUNE clé étrangère ni colonne liée aux utilisateurs**.
   - Un reçu cryptographique SHA-256 (`recu_vote_hash`) est délivré à l'électeur pour lui permettre de vérifier publiquement la présence de son bulletin dans l'urne sans révéler son choix.

## 3. Machine à États du Scrutin

Chaque élection traverse un cycle de vie contrôlé strictement côté serveur :

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Création du scrutin
    DRAFT --> CANDIDATURES_OPEN : Ouverture des candidatures
    DRAFT --> READY_TO_VOTE : Scrutin sans phase de candidature
    CANDIDATURES_OPEN --> CANDIDATURES_CLOSED : Clôture du dépôt
    CANDIDATURES_CLOSED --> VALIDATION : Instruction des dossiers
    VALIDATION --> READY_TO_VOTE : Liste officielle validée
    READY_TO_VOTE --> VOTING_OPEN : Date ouverture atteinte
    VOTING_OPEN --> VOTING_CLOSED : Date clôture atteinte
    VOTING_CLOSED --> COUNTING : Dépouillement officiel
    COUNTING --> RESULTS_PUBLISHED : Validation et diffusion
    RESULTS_PUBLISHED --> ARCHIVED : Archivage définitif
    ARCHIVED --> [*]
```

## 4. Spécifications Techniques

- **Langage Backend** : Java 21 LTS
- **Framework** : Spring Boot 3.3.5
- **Sécurité** : Spring Security 6, JJWT 0.12.6, Argon2id (Bouncy Castle)
- **Base de données** : PostgreSQL 16 & Flyway 10
- **Documentation API** : OpenAPI 3.0 / Swagger UI
- **Génération de Rapports** : OpenPDF 2.0
- **Temps Réel** : Spring WebSocket & STOMP Broker
- **Frontend** : Angular 19/21, Angular Material, TypeScript, PWA Service Worker
