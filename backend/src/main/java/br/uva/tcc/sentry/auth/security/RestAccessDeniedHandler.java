package br.uva.tcc.sentry.auth.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403 em JSON: o usuário está autenticado, mas o perfil dele não permite a ação. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        JsonErrorWriter.write(response, HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil não tem permissão para realizar esta ação.");
    }
}
