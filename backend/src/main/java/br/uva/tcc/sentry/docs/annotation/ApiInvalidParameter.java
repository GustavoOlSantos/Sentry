package br.uva.tcc.sentry.docs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 400 de parâmetro com tipo errado (ex.: UUID malformado no path ou valor de enum inexistente no query).
 * Use somente em endpoints que não têm outro 400 documentado — nos demais, descreva o 400 completo na própria interface.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(@ApiResponse(
    responseCode = "400",
    description = "Parâmetro inválido: o valor informado no path ou na query não tem o formato esperado (ex.: UUID malformado).",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class),
        examples = @ExampleObject(name = "Parâmetro inválido", value = ErrorExamples.INVALID_PARAMETER))))
public @interface ApiInvalidParameter {
}
