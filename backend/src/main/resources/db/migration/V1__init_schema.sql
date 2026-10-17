-- =============================================================================
-- Migration V1 : Schéma initial de la base de données VoteUASZ
-- Conforme aux exigences de sécurité, traçabilité et secret du scrutin
-- =============================================================================

-- 1. RÔLES
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 2. UTILISATEURS
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nom VARCHAR(80) NOT NULL,
    prenom VARCHAR(80) NOT NULL,
    numero_identification VARCHAR(50) UNIQUE, -- Numéro étudiant ou matricule personnel UASZ
    telephone VARCHAR(30),
    categorie VARCHAR(30) NOT NULL, -- ETUDIANT, PAT, ENSEIGNANT, ADMINISTRATIF
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    bloque BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    derniere_connexion TIMESTAMP WITH TIME ZONE
);

-- Association Utilisateurs - Rôles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- 3. COLLÈGES ÉLECTORAUX
CREATE TABLE IF NOT EXISTS colleges (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    ufr VARCHAR(100), -- Ex: UFR Sciences & Technologies, UFR Lettres & Sciences Humaines, etc.
    departement VARCHAR(100),
    niveau VARCHAR(50), -- Ex: Licence 1, Master, Tous
    categorie VARCHAR(30) NOT NULL -- ETUDIANT, ENSEIGNANT, PAT
);

-- 4. ÉLECTIONS
CREATE TABLE IF NOT EXISTS elections (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- DELEGUE, DIRECTEUR_UFR, VICE_RECTEUR, AUTRE
    college_id BIGINT NOT NULL,
    date_ouverture TIMESTAMP WITH TIME ZONE NOT NULL,
    date_cloture TIMESTAMP WITH TIME ZONE NOT NULL,
    statut VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    -- DRAFT, CANDIDATURES_OPEN, CANDIDATURES_CLOSED, VALIDATION, READY_TO_VOTE, VOTING_OPEN, VOTING_CLOSED, COUNTING, RESULTS_PUBLISHED, ARCHIVED
    permettre_vote_blanc BOOLEAN NOT NULL DEFAULT TRUE,
    description_regles TEXT,
    date_creation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_elections_college FOREIGN KEY (college_id) REFERENCES colleges (id)
);

-- 5. RÈGLES D'ÉLIGIBILITÉ DU SCRUTIN
CREATE TABLE IF NOT EXISTS election_rules (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL UNIQUE,
    ufr_cible VARCHAR(100),
    departement_cible VARCHAR(100),
    niveau_requis VARCHAR(50),
    categorie_requise VARCHAR(30),
    limite_age INT,
    regle_personnalisee_json TEXT,
    CONSTRAINT fk_rules_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE
);

-- 6. REGISTRE D'ÉLIGIBILITÉ ET CONSOMMATION DU DROIT DE VOTE (Séparation stricte)
-- Cette table sait SI l'électeur a voté, mais JAMAIS pour qui il a voté.
CREATE TABLE IF NOT EXISTS eligibility_records (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    eligible BOOLEAN NOT NULL DEFAULT TRUE,
    a_vote BOOLEAN NOT NULL DEFAULT FALSE,
    date_vote TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_eligibility_election_user UNIQUE (election_id, user_id),
    CONSTRAINT fk_eligibility_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE,
    CONSTRAINT fk_eligibility_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 7. CANDIDATURES
CREATE TABLE IF NOT EXISTS candidacies (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    candidat_id BIGINT NOT NULL,
    profession_foi TEXT,
    biographie TEXT,
    photo_url VARCHAR(255),
    statut VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, VALIDATED, REJECTED, WITHDRAWN
    motif_decision TEXT,
    date_decision TIMESTAMP WITH TIME ZONE,
    date_soumission TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidacies_election_user UNIQUE (election_id, candidat_id),
    CONSTRAINT fk_candidacies_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE,
    CONSTRAINT fk_candidacies_user FOREIGN KEY (candidat_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Documents justificatifs du candidat (CV, Attestations, Affiches)
CREATE TABLE IF NOT EXISTS candidate_documents (
    id BIGSERIAL PRIMARY KEY,
    candidacy_id BIGINT NOT NULL,
    type_document VARCHAR(50) NOT NULL, -- CV, ATTESTATION_INSCRIPTION, PROGRAMME, PHOTO, CASIER_JUDICIAIRE
    nom_fichier_original VARCHAR(255) NOT NULL,
    chemin_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets BIGINT NOT NULL,
    date_upload TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_docs_candidacy FOREIGN KEY (candidacy_id) REFERENCES candidacies (id) ON DELETE CASCADE
);

-- Contenus de campagne autorisés
CREATE TABLE IF NOT EXISTS campaign_contents (
    id BIGSERIAL PRIMARY KEY,
    candidacy_id BIGINT NOT NULL,
    titre VARCHAR(150) NOT NULL,
    texte TEXT,
    media_url VARCHAR(255),
    type_media VARCHAR(30), -- IMAGE, VIDEO_LINK, TEXTE
    statut_moderation VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    date_publication TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaign_candidacy FOREIGN KEY (candidacy_id) REFERENCES candidacies (id) ON DELETE CASCADE
);

-- 8. VÉRIFICATEURS OTP (Sécurité à usage unique)
CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    code_hache VARCHAR(255) NOT NULL, -- Hachage du code OTP, jamais en clair !
    date_expiration TIMESTAMP WITH TIME ZONE NOT NULL,
    tentatives_restantes INT NOT NULL DEFAULT 3,
    consomme BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_otp_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE
);

-- 9. JETONS D'AUTORISATION DE VOTE (Usage Unique Anonymisé)
-- Le jeton est émis après vérification OTP. Il ne contient aucun lien persistant avec le choix du vote.
CREATE TABLE IF NOT EXISTS vote_credentials (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 du jeton d'autorisation transmis
    election_id BIGINT NOT NULL,
    date_expiration TIMESTAMP WITH TIME ZONE NOT NULL,
    utilise BOOLEAN NOT NULL DEFAULT FALSE,
    date_utilisation TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_cred_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE
);

-- 10. URNE ÉLECTRONIQUE (Bulletins de vote strictly anonymisés)
-- AUCUNE CLÉ ÉTRANGÈRE VERS LA TABLE USERS !
CREATE TABLE IF NOT EXISTS ballots (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    candidacy_id BIGINT, -- NULL si vote blanc
    est_blanc BOOLEAN NOT NULL DEFAULT FALSE,
    recu_vote_hash VARCHAR(64) NOT NULL UNIQUE, -- Empreinte cryptographique du reçu délivré à l'électeur
    date_enregistrement TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ballots_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE,
    CONSTRAINT fk_ballots_candidacy FOREIGN KEY (candidacy_id) REFERENCES candidacies (id) ON DELETE SET NULL
);

-- 11. RÉSULTATS OFFICIELS DE DÉPOUILLEMENT
CREATE TABLE IF NOT EXISTS election_results (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL UNIQUE,
    inscrits_total INT NOT NULL DEFAULT 0,
    votants_total INT NOT NULL DEFAULT 0,
    bulletins_valides INT NOT NULL DEFAULT 0,
    bulletins_blancs INT NOT NULL DEFAULT 0,
    taux_participation DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    donnees_candidats_json TEXT NOT NULL, -- Détail voix et pourcentages
    statut_depouillement VARCHAR(40) NOT NULL, -- PROVISIONAL, VALIDATED, PUBLISHED
    date_depouillement TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_validation TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_results_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE
);

-- 12. JOURNAUX D'AUDIT STRUCTURÉS
CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_uuid VARCHAR(64) NOT NULL UNIQUE,
    horodatage TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(60) NOT NULL,
    acteur VARCHAR(120), -- Email de l'administrateur ou 'SYSTEM' (jamais l'électeur sur un vote !)
    election_id BIGINT,
    resultat VARCHAR(30) NOT NULL, -- SUCCESS, FAILURE, DENIED
    details TEXT,
    ip_source VARCHAR(60),
    correlation_id VARCHAR(64),
    empreinte_chaine VARCHAR(64) -- Chaînage d'intégrité (hash de l'événement et du hash précédent)
);

-- 13. GESTION DES RÉCLAMATIONS ET CONTESTATIONS
CREATE TABLE IF NOT EXISTS complaints (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    objet VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, UNDER_REVIEW, RESOLVED, REJECTED
    reponse_commission TEXT,
    pieces_jointes_urls TEXT,
    date_depot TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_traitement TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_complaints_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE,
    CONSTRAINT fk_complaints_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 14. PROCÈS-VERBAUX ET RAPPORTS OFFICIELS
CREATE TABLE IF NOT EXISTS official_reports (
    id BIGSERIAL PRIMARY KEY,
    numero_rapport VARCHAR(80) NOT NULL UNIQUE,
    election_id BIGINT NOT NULL,
    titre VARCHAR(200) NOT NULL,
    chemin_pdf VARCHAR(255) NOT NULL,
    empreinte_sha256 VARCHAR(64) NOT NULL,
    date_generation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    signataire_nom VARCHAR(120),
    signataire_role VARCHAR(80),
    CONSTRAINT fk_reports_election FOREIGN KEY (election_id) REFERENCES elections (id) ON DELETE CASCADE
);

-- INDEX POUR LES PERFORMANCES ET RECHERCHES RAPIDES
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_categorie ON users(categorie);
CREATE INDEX IF NOT EXISTS idx_elections_statut ON elections(statut);
CREATE INDEX IF NOT EXISTS idx_eligibility_lookup ON eligibility_records(election_id, user_id, a_vote);
CREATE INDEX IF NOT EXISTS idx_candidacies_lookup ON candidacies(election_id, statut);
CREATE INDEX IF NOT EXISTS idx_ballots_election ON ballots(election_id);
CREATE INDEX IF NOT EXISTS idx_audit_election_time ON audit_events(election_id, horodatage);
