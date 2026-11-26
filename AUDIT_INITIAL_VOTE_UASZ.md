# AUDIT INITIAL DU PROJET VOTE UASZ
**Date de réalisation :** 26 Novembre 2026  
**Auteur :** Architecte Logiciel Senior / Expert Cybersécurité  
**Projet :** Plateforme de Vote Électronique Universitaire — Université Assane Seck de Ziguinchor (UASZ)  
**Version de départ :** 1.0.0-SNAPSHOT  

---

## 1. ÉTAT INITIAL DES BUILDS ET DES TESTS

### 1.1 Backend (Spring Boot 3.3.5 / Java 21)
- **Compilation Maven :** `mvnw.cmd compile` et `mvnw.cmd test-compile` s'exécutent avec succès.
- **Suite de tests automatisés initiale :** 25 tests exécutés, 0 échec, 0 erreur (`BUILD SUCCESS`).
- **Tests exécutés :**
  1. `AuthenticationAndSecurityTests` (4 tests) : Inscription contrôlée `@zig.univ.sn`, hachage Argon2id, génération JWT, blocage des comptes inactifs.
  2. `EligibilityAndElectionWorkflowTests` (4 tests) : Machine à états des scrutins, calcul d'éligibilité par collège électoral.
  3. `FileSecurityAndComplaintsTests` (4 tests) : Validation des extensions et types MIME des justificatifs de candidature, workflow des réclamations.
  4. `OtpSecurityTests` (4 tests) : Expiration du défi OTP (300s), limitation à 3 tentatives, hash SHA-256 en base.
  5. `SecureVotingAndAntiDoubleVoteTests` (4 tests) : Protocole en 2 étapes, découplage de l'identité, rejet du double vote concurrentiel.
  6. `ResultsAndPdfReportTests` (4 tests) : Dépouillement des suffrages, publication des résultats, génération de PDF certifié, traçabilité sans fuite.
  7. `VoteUaszApplicationTests` (1 test) : Chargement du contexte Spring.

### 1.2 Frontend (Angular 21.2.0)
- **Compilation de production :** `npm run build` s'exécute avec code retour 0.
- **Avertissements relevés à la compilation initiale :**
  - Dépassement léger de budget de bundle initial (556 kB vs 500 kB).
  - 10 avertissements d'extended-diagnostics `NG8107` (opérateur optionnel inutile `?.` sur types non-nullables dans les templates).
  - Avertissement CommonJS sur `sockjs-client`.

---

## 2. INVENTAIRE DES ANOMALIES ET POINTS D'AMÉLIORATION IDENTIFIÉS

### [CRITIQUE 1 - RÈGLEMENT ÉLECTORAL & CERTIFICATION PDF]
- **Composant :** `ReportController.java` (`GET /reports/election/{electionId}/pdf`) et `PdfReportGeneratorService.java`.
- **Anomalie :** Le téléchargement et la génération du Procès-Verbal officiel au format PDF peuvent actuellement être invoqués pour une élection dont les résultats n'ont pas encore été validés et publiés (`RESULTS_PUBLISHED`). Un PV officiel portant la mention "CERTIFICATION DE CONFORMITÉ & SIGNATURES" ne doit jamais pouvoir être généré ou téléchargé publiquement tant que la commission n'a pas formellement publié les résultats.
- **Correction requise :** Ajouter un contrôle strict vérifiant que l'élection est en état `RESULTS_PUBLISHED` ou `ARCHIVED` avant de permettre la génération et le téléchargement public du PV officiel.

### [SÉCURITÉ 1 - CONTRÔLE D'ACCÈS DU TÉLÉCHARGEMENT PDF DANS SECURITYCONFIG]
- **Composant :** `SecurityConfig.java`.
- **Anomalie :** Le filtre autorise `GET /reports/verify/**` en accès public mais l'URL de téléchargement `/reports/election/**/pdf` n'est pas explicitement déclarée parmi les routes publiques dans `SecurityConfig`, bien que le contrôleur la destine aux électeurs et au public une fois l'élection publiée.
- **Correction requise :** Clarifier la règle dans `SecurityConfig` pour autoriser le téléchargement public du PDF officiel une fois le scrutin publié.

### [FONCTIONNALITÉ 1 - COMPLÉTION DU MODULE DE CAMPAGNE]
- **Composant :** Backend `CampaignController.java`, `CampaignService.java` & Frontend Angular.
- **Anomalie :** Le backend possède une entité `CampaignContent` et un contrôleur `CampaignController`, mais :
  1. Il n'existe pas d'endpoint permettant au candidat connecté de soumettre un article ou média de campagne pour sa propre candidature validée.
  2. Le frontend ne propose aucun onglet ou vue permettant aux électeurs de consulter la campagne/programme détaillée d'un candidat, ni au candidat de publier son message.
- **Correction requise :**
  - Ajouter l'endpoint `POST /campaigns/my/{candidacyId}` sécurisé (vérifiant que l'utilisateur connecté est bien le candidat titulaire de la candidature et que celle-ci est validée).
  - Intégrer dans le frontend Angular (`candidacies.component.ts`) l'affichage des contenus de campagne et un modal de publication pour le candidat.

### [SÉCURITÉ 2 - RENFORCEMENT DE L'INTÉGRITÉ DE L'AUDIT CONCURRENTIEL]
- **Composant :** `AuditService.java`.
- **Anomalie :** Le chaînage SHA-256 se base sur `auditRepository.findLastEvent()`. En cas d'événements simultanés quasi-parfaits, deux événements peuvent récupérer le même `previousHash`.
- **Correction requise :** Synchroniser l'accès au calcul du hash ou assurer la séquentialité stricte de l'empreinte pour garantir la linéarité absolue du grand livre cryptographique.

### [FRONTEND & UX/UI - NETTOYAGE ET ERGONOMIE]
- **Composant :** Templates Angular (`admin.component.ts`, `candidacies.component.ts`, `dashboard.component.ts`, `election-list.component.ts`).
- **Anomalies :**
  - 10 avertissements de diagnostic `NG8107` sur les chaînes de propriétés `e.college?.nom`.
  - Pas de lien direct vers les réclamations depuis la barre de navigation principale pour l'électeur connecté.
  - Gestion des feedbacks visuels d'erreur lors du vote ou du téléchargement du PV.
- **Correction requise :** Corriger les templates, ajouter l'accès Recours/Réclamations dans la navigation et enrichir les feedbacks utilisateurs.

---

## 3. PLAN D'ACTION MÉTHODIQUE

1. **Sauvegarde :** Enregistrement de l'état initial des fichiers et documentation de référence.
2. **Phase Backend & Sécurité :**
   - Sécurisation du `ReportController` et de `PdfReportGeneratorService` (rejet si scrutin non publié).
   - Ajout du point d'entrée d'écriture dans `CampaignController` et `CampaignService` avec vérification de propriété IDOR.
   - Alignement de `SecurityConfig.java`.
   - Sécurisation de l'accès au chaînage d'audit.
3. **Phase Frontend & UX/UI :**
   - Nettoyage des avertissements `NG8107`.
   - Ajout du lien "Réclamations & Recours" dans la navigation pour les utilisateurs connectés.
   - Ajout de la consultation des contenus de campagne dans `CandidaciesComponent`.
4. **Phase Tests & Vérification :**
   - Ajout de tests automatisés spécifiques couvrant les nouveaux scénarios (interdiction de PV avant publication, soumission de campagne autorisée uniquement au titulaire).
   - Exécution complète des tests Spring Boot et vérification du build Angular en production.
5. **Phase Documentation :**
   - Rédaction des 6 livrables obligatoires.
