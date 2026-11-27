package sn.uasz.voteuasz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalise automatiquement l'URL de connexion PostgreSQL/Supabase injectée par Render
 * (par exemple sous forme postgres://user:pass@host:port/db ou postgresql://...)
 * en URL JDBC standard acceptée par HikariCP et le pilote PostgreSQL.
 * Gère de manière robuste les mots de passe contenant des caractères spéciaux comme '@'.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

    private static final Pattern DB_URL_PATTERN = Pattern.compile("^(?:postgres|postgresql)://(?:([^:]+):(.*)@)?([^:/]+)(?::(\\d+))?(?:/([^?]+))?(?:\\?(.*))?$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("DATABASE_URL");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        rawUrl = rawUrl.trim();

        // Si l'URL commence par postgres:// ou postgresql:// (sans jdbc:)
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                // Découpage sécurisé : après le schéma, trouver le dernier '@' avant le '/' de la base
                String schemeLess = rawUrl.substring(rawUrl.indexOf("://") + 3);

                String userInfo = null;
                String rest = schemeLess;

                int firstSlash = schemeLess.indexOf('/');
                int lastAtBeforeSlash = (firstSlash != -1) 
                        ? schemeLess.substring(0, firstSlash).lastIndexOf('@') 
                        : schemeLess.lastIndexOf('@');

                if (lastAtBeforeSlash != -1) {
                    userInfo = schemeLess.substring(0, lastAtBeforeSlash);
                    rest = schemeLess.substring(lastAtBeforeSlash + 1);
                }

                String hostPort = rest;
                String pathAndQuery = "";
                int slashIdx = rest.indexOf('/');
                if (slashIdx != -1) {
                    hostPort = rest.substring(0, slashIdx);
                    pathAndQuery = rest.substring(slashIdx + 1);
                }

                String host = hostPort;
                int port = 5432;
                int colonIdx = hostPort.lastIndexOf(':');
                if (colonIdx != -1) {
                    host = hostPort.substring(0, colonIdx);
                    try {
                        port = Integer.parseInt(hostPort.substring(colonIdx + 1));
                    } catch (NumberFormatException ignored) {
                        port = 5432;
                    }
                }

                String dbName = "postgres";
                String query = null;
                int qMarkIdx = pathAndQuery.indexOf('?');
                if (qMarkIdx != -1) {
                    dbName = pathAndQuery.substring(0, qMarkIdx);
                    query = pathAndQuery.substring(qMarkIdx + 1);
                } else if (!pathAndQuery.isBlank()) {
                    dbName = pathAndQuery;
                }

                if (dbName.isBlank()) {
                    dbName = "postgres";
                }

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(dbName);

                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                    if (!query.contains("sslmode=")) {
                        jdbcUrl.append("&sslmode=require");
                    }
                } else {
                    jdbcUrl.append("?sslmode=require");
                }

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl.toString());

                if (userInfo != null && !userInfo.isBlank()) {
                    int firstColon = userInfo.indexOf(':');
                    String username;
                    String password = null;
                    if (firstColon != -1) {
                        username = userInfo.substring(0, firstColon);
                        password = userInfo.substring(firstColon + 1);
                    } else {
                        username = userInfo;
                    }

                    if (!username.isBlank()) {
                        String existingUser = environment.getProperty("SPRING_DATASOURCE_USERNAME");
                        if (existingUser == null || existingUser.isBlank() || "voteuasz_user".equals(existingUser)) {
                            props.put("spring.datasource.username", username);
                        }
                    }
                    if (password != null && !password.isBlank()) {
                        String existingPass = environment.getProperty("SPRING_DATASOURCE_PASSWORD");
                        if (existingPass == null || existingPass.isBlank()) {
                            props.put("spring.datasource.password", password);
                        }
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource("supabaseUrlOverrides", props));
                log.info("URL de base de données PostgreSQL/Supabase normalisée avec succès vers JDBC : jdbc:postgresql://{}:{}/{}", host, port, dbName);
            } catch (Exception e) {
                log.warn("Impossible de normaliser automatiquement l'URL de base de données : {}", e.getMessage());
            }
        }
    }
}
