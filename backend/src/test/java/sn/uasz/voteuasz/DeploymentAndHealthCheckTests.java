package sn.uasz.voteuasz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import sn.uasz.voteuasz.common.DatabaseUrlEnvironmentPostProcessor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de robustesse du déploiement et des configurations Render")
class DeploymentAndHealthCheckTests {

    private final DatabaseUrlEnvironmentPostProcessor postProcessor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("Normalisation réussie d'une URL Supabase de type postgres:// avec identifiants")
    void testNormalizeSupabaseUrlWithPostgresScheme() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgres://postgres.abc:SecretPass123@aws-0-eu-central-1.pooler.supabase.com:6543/postgres");

        postProcessor.postProcessEnvironment(env, new SpringApplication());

        String jdbcUrl = env.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:6543/postgres?sslmode=require", jdbcUrl);
        assertEquals("postgres.abc", env.getProperty("spring.datasource.username"));
        assertEquals("SecretPass123", env.getProperty("spring.datasource.password"));
    }

    @Test
    @DisplayName("Normalisation réussie d'une URL de type postgresql:// avec sslmode déjà présent")
    void testNormalizeUrlWithPostgresqlSchemeAndSsl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SPRING_DATASOURCE_URL", "postgresql://admin_user:P@ssword!@db.uasz.sn:5432/voteuasz_db?sslmode=prefer");

        postProcessor.postProcessEnvironment(env, new SpringApplication());

        String jdbcUrl = env.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://db.uasz.sn:5432/voteuasz_db?sslmode=prefer", jdbcUrl);
        assertEquals("admin_user", env.getProperty("spring.datasource.username"));
        assertEquals("P@ssword!", env.getProperty("spring.datasource.password"));
    }

    @Test
    @DisplayName("Ne modifie pas une URL déjà au format JDBC")
    void testIgnoreStandardJdbcUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/voteuasz_db?sslmode=disable");

        postProcessor.postProcessEnvironment(env, new SpringApplication());

        // L'URL JDBC reste inchangée
        assertNull(env.getProperty("spring.datasource.username"));
    }
}
