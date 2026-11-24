package br.uva.tcc.sentry.docs.api;

import java.util.List;

import org.springframework.http.ResponseEntity;

import br.uva.tcc.sentry.docs.annotation.ApiForbidden;
import br.uva.tcc.sentry.docs.annotation.ApiInvalidParameter;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.example.PolicyExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Documentação de {@code /policyRule}. Implementada por {@code PolicyRuleController}. */
@Tag(name = ApiTags.POLICY_RULES)
public interface PolicyRuleApi {

    @Operation(
        operationId = "createPolicyRule",
        summary = "Criar regra de política",
        description = """
            Cria uma regra vinculada a uma política existente (`policy.id`).

            - **Acesso:** somente `ADM`.
            - `type` define como `expectedValue` e `condition` são interpretados:
              - `FORBIDDEN_SERVICE`: `expectedValue` é um trecho do nome do serviço/produto proibido (ex.: `telnet`), \
            comparado sem diferenciar maiúsculas/minúsculas.
              - `PORT_RESTRICTION`: `expectedValue` é o número da porta que não pode estar aberta (ex.: `3389`); \
            `condition` (opcional) restringe o protocolo (ex.: `tcp`).
            - `severity`: `INFO`, `NONE`, `LOW`, `MEDIUM`, `HIGH` ou `CRITICAL`. Só regras com `enabled = true` de políticas `ACTIVE` são avaliadas.
            - `policy`, `name` e `type` são obrigatórios; a ausência (ou uma política inexistente) não é validada pela API \
            e resulta em erro **500** (violação de integridade).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Regra criada.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PolicyRule.class),
                examples = @ExampleObject(name = "Regra criada", value = PolicyExamples.RULE))),
        @ApiResponse(responseCode = "400", description = "Corpo ausente/malformado ou valor de enum inexistente (`type`, `severity`).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY))),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo política ausente/inexistente ou campo obrigatório ausente (violação de integridade no banco).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<PolicyRule> createPolicyRule(
        @RequestBody(description = "Dados da regra.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PolicyRule.class),
                examples = {
                    @ExampleObject(name = "Serviço proibido", value = PolicyExamples.RULE_REQUEST_FORBIDDEN_SERVICE),
                    @ExampleObject(name = "Restrição de porta", value = PolicyExamples.RULE_REQUEST_PORT_RESTRICTION)
                }))
        PolicyRule policyRule);

    @Operation(
        operationId = "listPolicyRules",
        summary = "Listar regras de política",
        description = "- **Acesso:** `ADM`, `DEV` ou `USER` (leitura).",
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Lista de regras.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PolicyRule.class)))))
    @ApiUnauthorized
    ResponseEntity<List<PolicyRule>> findAllPolicyRules();

    @Operation(
        operationId = "getPolicyRuleById",
        summary = "Buscar regra por id",
        description = "- **Acesso:** `ADM`, `DEV` ou `USER` (leitura).",
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Regra encontrada.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PolicyRule.class),
                examples = @ExampleObject(name = "Regra", value = PolicyExamples.RULE))),
        @ApiResponse(responseCode = "404", description = "Regra não encontrada (resposta **sem corpo**).", content = @Content)
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    ResponseEntity<PolicyRule> findById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador da regra.", example = "1") Long id);

    @Operation(
        operationId = "updatePolicyRule",
        summary = "Editar regra de política",
        description = """
            Substitui os dados da regra pelo corpo enviado (o `id` da URL prevalece sobre o do corpo).

            - **Acesso:** somente `ADM`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Regra atualizada.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PolicyRule.class),
                examples = @ExampleObject(name = "Regra atualizada", value = PolicyExamples.RULE))),
        @ApiResponse(responseCode = "400", description = "Corpo ausente/malformado, valor de enum inexistente ou `id` com formato inválido.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY),
                    @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                })),
        @ApiResponse(responseCode = "404", description = "Regra não encontrada (resposta **sem corpo**).", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo política ausente/inexistente (violação de integridade no banco).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<PolicyRule> editPolicyRule(
        @Parameter(in = ParameterIn.PATH, description = "Identificador da regra.", example = "1") Long id,
        @RequestBody(description = "Novos dados da regra.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PolicyRule.class),
                examples = @ExampleObject(name = "Edição", value = PolicyExamples.RULE_REQUEST_FORBIDDEN_SERVICE)))
        PolicyRule policyRule);

    @Operation(
        operationId = "deletePolicyRule",
        summary = "Excluir regra de política",
        description = """
            Exclui a regra.

            - **Acesso:** somente `ADM`.
            - Se houver registros que dependam da regra (ex.: avaliações e findings já gerados por ela), a violação de \
            integridade não é tratada pela aplicação e resulta em erro **500**.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Regra excluída (sem corpo)."),
        @ApiResponse(responseCode = "404", description = "Regra não encontrada (resposta **sem corpo**).", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo regra com registros dependentes (violação de integridade no banco).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<Void> deletePolicyRule(
        @Parameter(in = ParameterIn.PATH, description = "Identificador da regra.", example = "1") Long id);
}
