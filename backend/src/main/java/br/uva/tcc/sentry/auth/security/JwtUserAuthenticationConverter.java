package br.uva.tcc.sentry.auth.security;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Transforma um JWT já validado (assinatura, expiração, issuer) na autenticação
 * do Spring, consultando o usuário no banco a cada requisição. Isso troca um
 * pouco de desempenho por controle imediato:
 * <ul>
 *   <li>usuário desativado ou excluído é barrado na hora;</li>
 *   <li>a role usada é a ATUAL do banco, não a do momento do login;</li>
 *   <li>tokens emitidos antes da última troca de senha são recusados.</li>
 * </ul>
 * Qualquer recusa vira {@link InvalidBearerTokenException}, que o Spring Security
 * responde como 401.
 */
@Component
@RequiredArgsConstructor
public class JwtUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = userRepository.findById(parseUserId(jwt))
                .orElseThrow(() -> new InvalidBearerTokenException("Usuário do token não existe."));

        if (!user.isActive()) {
            throw new InvalidBearerTokenException("Usuário desativado.");
        }

        if (issuedBeforePasswordChange(jwt, user)) {
            throw new InvalidBearerTokenException("Token emitido antes da última troca de senha.");
        }

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(user.getRole().authority())),
                String.valueOf(user.getId()));
    }

    private Long parseUserId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token com subject inválido.");
        }
    }

    /** O {@code iat} do JWT tem precisão de segundos, então a comparação é feita em segundos. */
    private boolean issuedBeforePasswordChange(Jwt jwt, User user) {
        Instant issuedAt = jwt.getIssuedAt();
        if (issuedAt == null || user.getPasswordChangedAt() == null) {
            return false;
        }

        Instant passwordChangedAt = user.getPasswordChangedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .truncatedTo(ChronoUnit.SECONDS);

        return issuedAt.isBefore(passwordChangedAt);
    }
}
