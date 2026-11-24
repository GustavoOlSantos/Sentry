package br.uva.tcc.sentry.docs.model;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo padrão de erro da API. Existe somente para a documentação: quem de fato monta a
 * resposta é o {@code GlobalExceptionHandler} (exceções do MVC) e o {@code JsonErrorWriter}
 * (filtros de segurança: 401 e 403).
 */
@Schema(name = "ErrorResponse", description = "Corpo padrão de erro devolvido pela API.")
public record ApiErrorResponse(

    @Schema(description = "Momento em que o erro ocorreu (UTC).", example = "2026-11-22T14:30:00.123456Z")
    Instant timestamp,

    @Schema(description = "Código HTTP da resposta.", example = "400")
    int status,

    @Schema(description = "Título curto do erro.", example = "Dados inválidos")
    String error,

    @Schema(description = "Detalhe legível do erro. Em validações de campos, lista `campo: mensagem` separados por `; `.",
            example = "email: E-mail inválido.")
    String message
) {
}
