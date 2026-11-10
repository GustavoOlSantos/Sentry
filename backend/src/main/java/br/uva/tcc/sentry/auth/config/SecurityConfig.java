package br.uva.tcc.sentry.auth.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.uva.tcc.sentry.auth.security.JwtUserAuthenticationConverter;
import br.uva.tcc.sentry.auth.security.RestAccessDeniedHandler;
import br.uva.tcc.sentry.auth.security.RestAuthenticationEntryPoint;

/**
 * API stateless protegida por JWT (Bearer), com o Spring como Resource Server.
 *
 * Matriz de acesso — as regras são avaliadas na ordem e a primeira que casa vence:
 * <pre>
 *  Rota                                          Quem acessa
 *  ------------------------------------------    ------------------------------------
 *  OPTIONS /**                                   público (preflight do CORS)
 *  POST /auth/login                              público
 *  /actuator/health, /error                      público
 *  /v3/api-docs, /swagger-ui                     público se sentry.security.public-docs=true
 *  /users/**                                     ADM
 *  /auth/me, /auth/me/password                   qualquer usuário autenticado
 *  GET /asset, /findings, /scan, /policies,      ADM, DEV, USER (leitura)
 *      /reports
 *  POST /scan/exec                               ADM, DEV
 *  POST /findings/{id}/status                    ADM, DEV
 *  PATCH /findings/{id}/assignee                 ADM, DEV
 *  PUT /asset/{id}                               ADM, DEV
 *  DELETE /asset/{id}                            ADM
 *  escrita em /policies/**                       ADM
 *  qualquer outra rota                           ADM  (padrão seguro: rota nova nasce restrita)
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final String ADM = "ADM";
    private static final String DEV = "DEV";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityProperties properties,
                                            JwtUserAuthenticationConverter jwtConverter,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> {
                // ---- públicas ----
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                auth.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                auth.requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll();
                if (properties.publicDocs()) {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                }

                // ---- administração de usuários ----
                auth.requestMatchers("/users/**").hasRole(ADM);

                // ---- qualquer usuário autenticado ----
                auth.requestMatchers("/auth/me", "/auth/me/**").authenticated();

                // ---- leitura: ADM, DEV e USER ----
                auth.requestMatchers(HttpMethod.GET,
                        "/asset/**", "/findings/**", "/scan/**", "/policies/**", "/reports/**").authenticated();

                // ---- operação: ADM e DEV ----
                auth.requestMatchers(HttpMethod.POST, "/scan/exec").hasAnyRole(ADM, DEV);
                auth.requestMatchers(HttpMethod.POST, "/findings/*/status").hasAnyRole(ADM, DEV);
                auth.requestMatchers(HttpMethod.PATCH, "/findings/*/assignee").hasAnyRole(ADM, DEV);
                auth.requestMatchers(HttpMethod.PUT, "/asset/*").hasAnyRole(ADM, DEV);

                // ---- somente ADM ----
                auth.requestMatchers(HttpMethod.DELETE, "/asset/*").hasRole(ADM);
                auth.requestMatchers("/policies/**").hasRole(ADM); // escrita (o GET já foi liberado acima)

                // ---- padrão seguro ----
                auth.anyRequest().hasRole(ADM);
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        SecurityProperties.Cors cors = properties.cors();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false); // o token vai no header Authorization, não em cookie
        configuration.setMaxAge(cors.maxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // validações padrão (expiração, not-before) + issuer esperado
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
        return decoder;
    }

    /** Propositalmente não é um bean: assim a chave não fica injetável em qualquer lugar. */
    private SecretKey secretKey(SecurityProperties properties) {
        return new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
