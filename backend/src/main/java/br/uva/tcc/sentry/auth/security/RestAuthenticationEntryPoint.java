package br.uva.tcc.sentry.auth.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 em JSON quando não há token ou ele é inválido/expirado. A mensagem é
 * genérica de propósito: não diz ao cliente por que o token foi recusado.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        JsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "Não autenticado",
                "Autenticação necessária: token ausente, inválido ou expirado.");
    }
}
