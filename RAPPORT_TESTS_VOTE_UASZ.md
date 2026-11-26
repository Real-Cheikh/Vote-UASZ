# RAPPORT DES TESTS AUTOMATISÉS DU PROJET VOTE UASZ
**Date :** 26 Novembre 2026  
**Environnement de test :** Java 21 LTS / Spring Boot Test / H2 in-memory (Profil test)  
**Couverture :** Sécurité, Processus Électoral, Concurrence, Dépouillement, Workflow  

---

## 1. RÉSULTATS D'EXÉCUTION MAVEN SUREFIRE

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running sn.uasz.voteuasz.AuthenticationAndSecurityTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.112 s
[INFO] Running sn.uasz.voteuasz.EligibilityAndElectionWorkflowTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.845 s
[INFO] Running sn.uasz.voteuasz.FileSecurityAndComplaintsTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.982 s
[INFO] Running sn.uasz.voteuasz.OtpSecurityTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.498 s
[INFO] Running sn.uasz.voteuasz.ResultsAndPdfReportTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.451 s
[INFO] Running sn.uasz.voteuasz.SecureVotingAndAntiDoubleVoteTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.433 s
[INFO] Running sn.uasz.voteuasz.VoteUaszApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 2. VÉRIFICATION DES SCÉNARIOS CRITIQUES EXIGÉS

1. **Un utilisateur non authentifié ne peut pas voter :**  
   - *Vérifié :* Interception par `JwtAuthenticationFilter` et rejet HTTP 401/403.
2. **Un utilisateur non éligible ne peut pas voter :**  
   - *Vérifié :* `EligibilityService.checkEligibility` lève une exception `VoteException`.
3. **Un électeur ne peut pas voter deux fois :**  
   - *Vérifié :* Test `testSequentialDoubleVoteRejection` valide le refus dès la 2e tentative avec statut `DENIED`.
4. **Deux requêtes simultanées ne peuvent pas produire deux votes pour le même électeur :**  
   - *Vérifié :* Test concurrentiel `testConcurrentVoteConsumptionsRejected` (threads concurrents) : un seul vote est admis, l'autre est rejeté par verrou et mise à jour atomique.
5. **Un candidat ne peut pas modifier les données d'un autre candidat :**  
   - *Vérifié :* Contrôle de propriété du `UserPrincipal` dans `CampaignService` et `CandidacyService`.
6. **Un électeur ne peut pas accéder aux fonctions administratives :**  
   - *Vérifié :* `@PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")` sur `/admin/**`, `/audit/**` et `/users/**`.
7. **Un résultat non publié ne doit pas être exposé publiquement :**  
   - *Vérifié :* `ResultsService.getPublicResults` lève `BusinessException` si l'élection n'est pas en `RESULTS_PUBLISHED` ou `ARCHIVED`.
8. **Un bulletin ne doit pas révéler l'identité de son auteur :**  
   - *Vérifié :* Schéma de base `ballots` sans colonne d'identification utilisateur.
9. **Un administrateur non autorisé ne peut pas modifier les bulletins :**  
   - *Vérifié :* Aucune API de mise à jour (`UPDATE`) n'existe sur la table `ballots`.
10. **Un procès-verbal doit correspondre aux résultats enregistrés :**  
    - *Vérifié :* `PdfReportGeneratorService` s'appuie directement sur les totaux calculés par `ResultsService` et intègre un contrôle d'état obligatoire avant certification.

---

## 3. COMPILATION DE PRODUCTION FRONTEND ANGULAR

```
Initial chunk files | Names   | Raw size  | Estimated transfer size
main-VRICK4B6.js    | main    | 551.22 kB | 134.60 kB
styles-44YQAGVI.css | styles  | 4.98 kB   | 1.30 kB
Application bundle generation complete. [0 ERRORS]
```
- Avertissements de compilation Angular corrigés avec succès.
