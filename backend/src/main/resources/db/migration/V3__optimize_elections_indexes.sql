-- ============================================================================
-- V3__optimize_elections_indexes.sql
-- Optimisation des performances des requêtes de consultation des scrutins
-- VoteUASZ - Université Assane Seck de Ziguinchor
-- ============================================================================

-- 1. Index composite couvrant le statut et le tri décroissant sur la date d'ouverture
-- Évite le sequential scan et le tri en mémoire (quicksort) sur les requêtes publiques
CREATE INDEX IF NOT EXISTS idx_elections_statut_date
    ON elections (statut, date_ouverture DESC);

-- 2. Index sur la clé étrangère du collège électoral
-- Accélère les jointures entre la table elections et la table colleges
CREATE INDEX IF NOT EXISTS idx_elections_college_id
    ON elections (college_id);

-- 3. Index sur la date de clôture
-- Optimise les requêtes de recherche des scrutins en cours de vote et vérification d'expiration
CREATE INDEX IF NOT EXISTS idx_elections_date_cloture
    ON elections (date_cloture ASC);

-- 4. Index sur la clé étrangère election_id dans election_rules si non existant
CREATE INDEX IF NOT EXISTS idx_election_rules_election_id
    ON election_rules (election_id);
