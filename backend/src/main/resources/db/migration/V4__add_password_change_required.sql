-- =============================================================================
-- Migration V4 : Obligation de renouvellement de mot de passe à la première connexion
-- et traçabilité de l'initialisation des comptes officiels
-- =============================================================================

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS doit_changer_mot_de_passe BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.doit_changer_mot_de_passe IS 'Indicateur contraignant obligeant le renouvellement du mot de passe initial à la première connexion';
