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
import br.uva.tcc.sentry.policy.domain.Policy;
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

/** Documentação de {@code /policy}. Implementada por {@code PolicyController}. */
@Tag(name = ApiTags.POLICIES)
public interface PolicyApi {

    @Operation(
        operationId = "createPolicy",
        summary = "Criar política",
        description = """
            Cria uma política de segurança. As regras (`/policyRule`) são vinculadas a ela depois.

            - **Acesso:** somente `ADM`.
            - `name` e `status` são obrigatórios; `status`: `ACTIVE` ou `INACTIVE`. Só políticas `ACTIVE` são avaliadas nos scans.
            - `id`, `createdAt` e `updatedAt` são gerados pelo sistema.
            - Campo obrigatório ausente não é validado pela API: a violação de integridade do banco resulta em erro **500**.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Política criada.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Policy.class),
                examples = @ExampleObject(name = "Política criada", value = PolicyExamples.POLICY))),
        @ApiResponse(responseCode = "400", description = "Corpo ausente/malformado ou valor de enum inexistente (ex.: `status`).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY))),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo campo obrigatório ausente (violação de integridade no banco).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<Policy> createPolicy(
        @RequestBody(description = "Dados da política.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Policy.class),
                examples = @ExampleObject(name = "Nova política", value = PolicyExamples.POLICY_REQUEST)))
        Policy policy);

    @Operation(
        operationId = "listPolicies",
        summary = "Listar políticas",
        description = "- **Acesso:** `ADM`, `DEV` ou `USER` (leitura).",
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Lista de políticas.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = Policy.class)),
                examples = @ExampleObject(name = "Políticas", value = PolicyExamples.POLICY_LIST))))
    @ApiUnauthorized
    ResponseEntity<List<Policy>> findAllPolicies();

    @Operation(
        operationId = "getPolicyById",
        summary = "Buscar política por id",
        description = "- **Acesso:** `ADM`, `DEV` ou `USER` (leitura).",
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Política encontrada.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Policy.class),
                examples = @ExampleObject(name = "Política", value = PolicyExamples.POLICY))),
        @ApiResponse(responseCode = "404", description = "Política não encontrada (resposta **sem corpo**).", content = @Content)
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    ResponseEntity<Policy> findById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador da política.", example = "1") Long id);

    @Operation(
        operationId = "updatePolicy",
        summary = "Editar política",
        description = """
            Substitui os dados da política pelo corpo enviado (o `id` da URL prevalece sobre o do corpo).

            - **Acesso:** somente `ADM`.
            - `createdAt` não é alterado; `updatedAt` é atualizado automaticamente.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Política atualizada.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Policy.class),
                examples = @ExampleObject(name = "Política atualizada", value = PolicyExamples.POLICY))),
        @ApiResponse(responseCode = "400", description = "Corpo ausente/malformado, valor de enum inexistente ou `id` com formato inválido.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY),
                    @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                })),
        @ApiResponse(responseCode = "404", description = "Política não encontrada (resposta **sem corpo**).", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo campo obrigatório ausente (violação de integridade no banco).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<Policy> editPolicy(
        @Parameter(in = ParameterIn.PATH, description = "Identificador da política.", example = "1") Long id,
        @RequestBody(description = "Novos dados da política.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Policy.class),
                examples = @ExampleObject(name = "Edição", value = PolicyExamples.POLICY_REQUEST)))
        Policy policy);

    @Operation(
        operationId = "deletePolicy",
        summary = "Excluir política",
        description = """
            Exclui a política.

            - **Acesso:** somente `ADM`.
            - Se a política tiver regras vinculadas (ou outros registros que dependam dela), a violação de integridade \
            não é tratada pela aplicação e resulta em erro **500**.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Política excluída (sem corpo)."),
        @ApiResponse(responseCode = "404", description = "Política não encontrada (resposta **sem corpo**).", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo política com regras vinculadas (violação de integridade no banco).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<Void> deletePolicy(
        @Parameter(in = ParameterIn.PATH, description = "Identificador da política.", example = "1") Long id);
}
