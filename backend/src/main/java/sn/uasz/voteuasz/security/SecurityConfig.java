package sn.uasz.voteuasz.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:8080,https://*.vercel.app,https://vote-uasz-ojy5.vercel.app,https://vote-uasz-git-main-real-cheikh.vercel.app}") String[] allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOrigins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics (authentification, consultation basique, vérification)
                        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/elections/public", "/elections/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/candidacies/public", "/candidacies/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/candidacies/documents/file/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/results/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/voting/verify-receipt/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reports/verify/**").permitAll()

                        // Documentation OpenAPI / Swagger UI
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Actuator (santé du service)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // WebSocket STOMP
                        .requestMatchers("/ws/**").permitAll()

                        // Endpoints d'administration globale
                        .requestMatchers("/admin/**", "/audit/**").hasRole("SUPER_ADMIN_ELECTORAL")

                        // Endpoints Commission Électorale
                        .requestMatchers("/elections/manage/**", "/candidacies/review/**", "/results/count/**", "/results/publish/**", "/reports/generate/**")
                        .hasAnyRole("COMMISSION_ELECTORALE", "SUPER_ADMIN_ELECTORAL")

                        // Candidats (gestion de leur dossier et campagne)
                        .requestMatchers("/candidacies/apply", "/candidacies/my", "/candidacies/my/**", "/campaigns/my/**", "/candidacies/open-elections")
                        .hasAnyRole("CANDIDAT", "ELECTEUR", "COMMISSION_ELECTORALE", "SUPER_ADMIN_ELECTORAL")

                        // Tous les autres endpoints requièrent une authentification valide
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(this.allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
