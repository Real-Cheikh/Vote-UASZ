package sn.uasz.voteuasz.security;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Permet de répondre avec succès aux sondes de santé de Render ou des load balancers
 * sur les chemins racines (/actuator/health, /health, /) même lorsque l'application
 * Spring Boot est déployée sous le context-path '/api/v1'.
 */
@Configuration
public class TomcatHealthCheckConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHealthCheckCustomizer() {
        return factory -> factory.addEngineValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                String uri = request.getRequestURI();
                if ("/actuator/health".equals(uri) || "/health".equals(uri) || "/actuator/info".equals(uri)) {
                    response.setStatus(200);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":\"UP\",\"service\":\"voteuasz-backend\"}");
                    return;
                }
                if ("/".equals(uri) || "".equals(uri)) {
                    response.setStatus(200);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":\"UP\",\"name\":\"VoteUASZ API\",\"version\":\"1.0.0\"}");
                    return;
                }
                getNext().invoke(request, response);
            }
        });
    }
}
