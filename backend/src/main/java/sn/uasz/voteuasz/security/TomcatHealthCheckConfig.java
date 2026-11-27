package sn.uasz.voteuasz.security;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Routeur de vérification de santé au niveau du moteur Tomcat.
 * Si une sonde externe (Koyeb, Render, Load Balancer) interroge le chemin racine
 * (/actuator/health ou /health) en dehors du servlet context-path '/api/v1',
 * cette valve délègue STRICTEMENT au HealthEndpoint réel de Spring Boot Actuator
 * (qui sonde l'accès réel à la base de données PostgreSQL/Supabase, l'espace disque, etc.).
 *
 * Elle ne renvoie JAMAIS de statut 'UP' artificiel si la base de données ou un composant
 * critique est indisponible.
 */
@Configuration
public class TomcatHealthCheckConfig {

    private final ObjectProvider<HealthEndpoint> healthEndpointProvider;

    public TomcatHealthCheckConfig(ObjectProvider<HealthEndpoint> healthEndpointProvider) {
        this.healthEndpointProvider = healthEndpointProvider;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHealthCheckCustomizer() {
        return factory -> factory.addEngineValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                String uri = request.getRequestURI();

                if ("/actuator/health".equals(uri) || "/health".equals(uri)) {
                    HealthEndpoint healthEndpoint = healthEndpointProvider.getIfAvailable();
                    if (healthEndpoint == null) {
                        response.setStatus(503);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\":\"INITIALIZING\",\"details\":\"Le sous-système de santé Actuator est en cours de démarrage.\"}");
                        return;
                    }

                    HealthComponent health = healthEndpoint.health();
                    boolean isUp = Status.UP.equals(health.getStatus());

                    response.setStatus(isUp ? 200 : 503);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":\"" + health.getStatus().getCode() + "\"}");
                    return;
                }

                if ("/actuator/info".equals(uri)) {
                    response.setStatus(200);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"app\":\"VoteUASZ-Backend\",\"version\":\"1.0.0\"}");
                    return;
                }

                if ("/".equals(uri) || "".equals(uri)) {
                    HealthEndpoint healthEndpoint = healthEndpointProvider.getIfAvailable();
                    boolean isUp = healthEndpoint != null && Status.UP.equals(healthEndpoint.health().getStatus());
                    response.setStatus(isUp ? 200 : 503);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":\"" + (isUp ? "UP" : "DEGRADED") + "\",\"name\":\"VoteUASZ API\",\"version\":\"1.0.0\"}");
                    return;
                }

                getNext().invoke(request, response);
            }
        });
    }
}
