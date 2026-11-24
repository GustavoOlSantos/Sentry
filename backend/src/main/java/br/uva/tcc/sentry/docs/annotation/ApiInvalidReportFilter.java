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

/** 400 comum a todos os endpoints de relatório: filtro de período inválido ou parâmetro com tipo errado. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(@ApiResponse(
    responseCode = "400",
    description = "Filtro inválido: data inicial depois da final, período acima do máximo (366 dias) "
            + "ou parâmetro com formato/valor inválido (data fora de `yyyy-MM-dd`, UUID malformado, enum inexistente).",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class),
        examples = {
            @ExampleObject(name = "Datas invertidas", value = ErrorExamples.INVALID_REPORT_FILTER_ORDER),
            @ExampleObject(name = "Período acima do máximo", value = ErrorExamples.INVALID_REPORT_FILTER_RANGE),
            @ExampleObject(name = "Parâmetro inválido", value = ErrorExamples.INVALID_PARAMETER_QUERY)
        })))
public @interface ApiInvalidReportFilter {
}
