package br.uva.tcc.sentry.docs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/** 401 devolvido pelo filtro de segurança em qualquer rota protegida (token ausente, inválido ou expirado). */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(@ApiResponse(
    responseCode = "401",
    description = "Não autenticado: token ausente, inválido ou expirado — ou usuário do token desativado/excluído, "
            + "ou senha trocada depois da emissão do token.",
    headers = @Header(name = "WWW-Authenticate", description = "Esquema de autenticação esperado.",
            schema = @Schema(type = "string", example = "Bearer")),
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class),
        examples = @ExampleObject(name = "Não autenticado", value = ErrorExamples.UNAUTHENTICATED))))
public @interface ApiUnauthorized {
}
