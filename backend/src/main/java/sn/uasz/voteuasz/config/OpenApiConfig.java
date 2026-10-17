package sn.uasz.voteuasz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI voteUaszOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API Plateforme VoteUASZ")
                        .description("Spécification officielle de l'API REST de vote électronique pour l'Université Assane Seck de Ziguinchor (UASZ), Sénégal.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Direction des Systèmes d'Information - UASZ")
                                .email("elections@uasz.sn")
                                .url("https://www.uasz.sn"))
                        .license(new License().name("Usage Académique & Institutionnel UASZ")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez le jeton JWT d'accès Bearer obtenu lors de la connexion")));
    }
}
