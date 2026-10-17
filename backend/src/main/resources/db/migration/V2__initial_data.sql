-- =============================================================================
-- Migration V2 : Données initiales de référence (Rôles, Collèges électoraux UASZ)
-- =============================================================================

-- Insertion des 4 rôles majeurs du système VoteUASZ
INSERT INTO roles (name, description) VALUES
('ROLE_SUPER_ADMIN_ELECTORAL', 'Super-administrateur : gestion globale, rôles, configuration système et audit'),
('ROLE_COMMISSION_ELECTORALE', 'Commission électorale : organisation, validation scrutins/candidats, dépouillement'),
('ROLE_CANDIDAT', 'Candidat officiel : soumission dossier, profession de foi, campagne autorisée'),
('ROLE_ELECTEUR', 'Électeur : participation aux scrutins éligibles, vote anonyme vérifié, réclamations')
ON CONFLICT (name) DO NOTHING;

-- Insertion des Collèges Électoraux de référence de l'Université Assane Seck de Ziguinchor
INSERT INTO colleges (code, nom, description, ufr, departement, niveau, categorie) VALUES
('COL_UFR_ST_ETUD', 'Collège Étudiants - UFR Sciences et Technologies', 'Tous les étudiants inscrits à l''UFR Sciences et Technologies', 'UFR Sciences et Technologies', 'Tous', 'Tous cycles', 'ETUDIANT'),
('COL_UFR_LSH_ETUD', 'Collège Étudiants - UFR Lettres et Sciences Humaines', 'Tous les étudiants inscrits à l''UFR Lettres et Sciences Humaines', 'UFR Lettres et Sciences Humaines', 'Tous', 'Tous cycles', 'ETUDIANT'),
('COL_UFR_2S_ETUD', 'Collège Étudiants - UFR Sciences de la Santé', 'Tous les étudiants inscrits à l''UFR Sciences de la Santé', 'UFR Sciences de la Santé', 'Tous', 'Tous cycles', 'ETUDIANT'),
('COL_UFR_SES_ETUD', 'Collège Étudiants - UFR Sciences Économiques et Sociales', 'Tous les étudiants inscrits à l''UFR Sciences Économiques et Sociales', 'UFR Sciences Économiques et Sociales', 'Tous', 'Tous cycles', 'ETUDIANT'),
('COL_ENSEIGNANTS_UASZ', 'Collège Enseignants-Chercheurs UASZ', 'Corps enseignant et de recherche titulaire de l''UASZ', 'Toutes UFR', 'Tous', 'Permanent', 'ENSEIGNANT'),
('COL_PAT_UASZ', 'Collège Personnel Administratif et Technique (PAT)', 'Personnel administratif, technique et de service de l''UASZ', 'Services Centraux & UFR', 'Tous', 'Permanent', 'PAT')
ON CONFLICT (code) DO NOTHING;
