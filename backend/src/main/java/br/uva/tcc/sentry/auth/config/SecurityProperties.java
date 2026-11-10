package br.uva.tcc.sentry.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configurações de segurança (prefixo {@code sentry.security} no application.yaml).
 *
 * @param publicDocs libera o Swagger/OpenAPI sem login. Útil em desenvolvimento;
 *                   em produção defina {@code SENTRY_PUBLIC_DOCS=false}.
 */
@ConfigurationProperties(prefix = "sentry.security")
public record SecurityProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Cors cors,
        @DefaultValue("true") boolean publicDocs,
        @DefaultValue BootstrapAdmin bootstrapAdmin) {

    /** Tamanho mínimo da chave HS256: 256 bits. */
    public static final int MIN_SECRET_BYTES = 32;

    /**
     * @param secret     chave de assinatura HS256 (variável SENTRY_JWT_SECRET), mínimo 32 bytes.
     *                   Gere uma com {@code openssl rand -base64 48}.
     * @param expiration validade do token (ISO-8601, ex.: PT2H)
     */
    public record Jwt(String secret, @DefaultValue("PT2H") Duration expiration, @DefaultValue("sentry") String issuer) {

        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                        "sentry.security.jwt.secret é obrigatório e precisa ter ao menos " + MIN_SECRET_BYTES
                        + " caracteres. Defina a variável de ambiente SENTRY_JWT_SECRET "
                        + "(ex.: openssl rand -base64 48).");
            }
            if (expiration == null || expiration.isZero() || expiration.isNegative()) {
                throw new IllegalStateException("sentry.security.jwt.expiration deve ser uma duração positiva (ex.: PT2H).");
            }
        }
    }

    /**
     * @param allowedOrigins origens autorizadas a chamar a API pelo navegador
     *                       (variável SENTRY_CORS_ALLOWED_ORIGINS, separadas por vírgula)
     */
    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins,
                       @DefaultValue("3600") long maxAgeSeconds) {
    }

    /**
     * Administrador criado na primeira inicialização, quando ainda não existe nenhum usuário.
     * Sem e-mail e senha definidos, nada é criado.
     */
    public record BootstrapAdmin(@DefaultValue("Administrador") String name,
                                 String email,
                                 String password) {

        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
