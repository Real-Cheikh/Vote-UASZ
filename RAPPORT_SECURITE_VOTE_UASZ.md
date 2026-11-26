# RAPPORT DE SÉCURITÉ DU SYSTÈME VOTE UASZ
**Date :** 26 Novembre 2026  
**Auditeur :** Architecte Full Stack & Expert Cybersécurité  
**Système :** Plateforme de Vote Électronique Universitaire UASZ  

---

## 1. Analyse Inspirée de l'OWASP Top 10

### A01:2021 – Broken Access Control (Contrôle d'accès)
- **Mesures en place :**
  - Contrôle granulaire par rôles (`ROLE_SUPER_ADMIN_ELECTORAL`, `ROLE_COMMISSION_ELECTORALE`, `ROLE_CANDIDAT`, `ROLE_ELECTEUR`) dans `SecurityConfig.java` et par annotations `@PreAuthorize`.
  - Protection IDOR sur les dossiers de candidatures et les messages de campagne : vérification que `candidacy.getCandidat().getId()` correspond à l'utilisateur authentifié issu du token JWT.
  - Verrouillage du téléchargement et de la génération des Procès-Verbaux officiels avant la publication formelle des résultats.

### A02:2021 – Cryptographic Failures (Défaillances cryptographiques)
- **Mesures en place :**
  - Hachage de mot de passe par **Argon2id** (Bouncy Castle) avec mémoire 64MB, 3 itérations, 1 thread (résistance aux attaques GPU/ASIC).
  - Signature des JWT par HMAC-SHA256 avec clé robuste, expiration courte (15 minutes).
  - Codes OTP hachés en SHA-256 dans la base de données (jamais stockés en clair).
  - Grand livre d'audit chaîné avec empreinte SHA-256 cumulative.

### A03:2021 – Injection (Injections SQL & Commandes)
- **Mesures en place :**
  - Requêtes paramétrées via Spring Data JPA / Hibernate et requêtes JPQL sécurisées.
  - Aucun usage de concaténation de chaînes dans les requêtes natives.
  - Validation stricte des données d'entrée par Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`).

### A04:2021 – Insecure Design (Conception non sécurisée du vote)
- **Principe du découplage anonyme en 2 étapes :**
  1. Étape 1 : Vérification d'éligibilité, consommation atomique de `a_vote = true` et émission d'un `VoteCredential` éphémère (15 minutes).
  2. Étape 2 : Consommation atomique du jeton et dépôt d'un bulletin dans la table `ballots`.
  3. **Isolation mathématique :** La table `ballots` ne contient aucune colonne `user_id`, ni clé étrangère vers `users`.
  4. Récépissé cryptographique SHA-256 généré aléatoirement avec sel temporel, permettant à l'électeur de vérifier la présence de son bulletin sans révéler son choix.

### A05:2021 – Security Misconfiguration (Mauvaise configuration)
- **CORS :** Configuration restrictive autorisant uniquement les origines universitaires et les domaines officiels déclarés via `CORS_ALLOWED_ORIGINS`.
- **En-têtes de sécurité :** Cookies et sessions stateless (`SessionCreationPolicy.STATELESS`).

### A06:2021 – Vulnerable and Outdated Components
- Stack moderne maintenue : Spring Boot 3.3.5, Java 21 LTS, JJWT 0.12.6, Angular 21.2.0, OpenPDF 2.0.3.

### A07:2021 – Identification and Authentication Failures
- Blocage strict de l'enregistrement aux adresses de courriel officielles du domaine universitaire (`@zig.univ.sn` et `@uasz.sn`).
- Limitation des tentatives OTP (3 tentatives max, expiration à 300 secondes).
- Détection immédiate des comptes inactifs (`is_active = false`).

### A08:2021 – Software and Data Integrity Failures
- Migrations de bases de données immuables versionnées avec Flyway (`V1`, `V2`, `V3`).
- Signature et vérification d'empreinte SHA-256 sur les fichiers PDF générés.
- Téléversement de pièces jointes protégé contre le Path Traversal (`..`), validation stricte de l'extension et des types MIME autorisés (PDF, PNG, JPG).
