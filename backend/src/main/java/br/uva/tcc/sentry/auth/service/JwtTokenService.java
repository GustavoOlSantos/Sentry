package br.uva.tcc.sentry.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.auth.config.SecurityProperties;
import br.uva.tcc.sentry.auth.domain.User;
import lombok.RequiredArgsConstructor;

/**
 * Emite JWTs assinados com HS256.
 *
 * O token carrega o id do usuário no {@code sub}. A role vai como claim apenas
 * para o frontend poder ajustar a interface: quem decide o acesso é o
 * {@code JwtUserAuthenticationConverter}, que lê a role ATUAL do banco — assim
 * trocar o perfil ou desativar um usuário vale na hora, sem esperar o token expirar.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    @Override
    public IssuedToken issue(User user) {
        SecurityProperties.Jwt jwt = properties.jwt();

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwt.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwt.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, expiresAt, jwt.expiration().toSeconds());
    }
}
