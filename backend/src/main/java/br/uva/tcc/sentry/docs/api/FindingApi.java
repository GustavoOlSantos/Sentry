package br.uva.tcc.sentry.docs.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import br.uva.tcc.sentry.docs.annotation.ApiForbidden;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.example.FindingExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingAssigneeRequest;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingAssigneeResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusRequest;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusResponse;
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

/** Documentação de {@code /findings}. Implementada por {@code FindingController}. */
@Tag(name = ApiTags.FINDINGS)
public interface FindingApi {

    @Operation(
        operationId = "listFindings",
        summary = "Listar findings (board)",
        description = """
            Lista todos os findings no formato usado pelo board, com o asset e o serviço de cada um.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            - `type`: `VULNERABILITY` ou `POLICY_VIOLATION`.
            - `severity`: `INFO`, `NONE`, `LOW`, `MEDIUM`, `HIGH` ou `CRITICAL`.
            - `status`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `VALIDATED`, `JUSTIFIED` ou `FAILED_VALIDATION`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Lista de findings.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FindingBoardResponse.class)),
                examples = @ExampleObject(name = "Board", value = FindingExamples.FINDING_BOARD))))
    @ApiUnauthorized
    List<FindingBoardResponse> findAll();

    @Operation(
        operationId = "updateFindingStatus",
        summary = "Alterar o status de um finding",
        description = """
            Muda o status do finding (movimentação no board) e registra a transição no histórico, com o usuário que a fez.

            - **Acesso:** `ADM` ou `DEV`.
            - Status que o usuário pode definir: `OPEN`, `IN_PROGRESS`, `RESOLVED` e `JUSTIFIED`.
            - `VALIDATED` e `FAILED_VALIDATION` são **exclusivos da ferramenta** (resultado dos retestes) e retornam 400.
            - `JUSTIFIED` (risco aceito) exige `justification` preenchida.
            - `RESOLVED` solicita automaticamente um **reteste** para validar a correção (a mensagem da resposta informa isso).
            - Findings já `VALIDATED` (fechados) não podem ser alterados manualmente (409); se o problema voltar, o próximo scan o reabre.

            **Atenção ao formato dos erros 400:** as validações de `status`/`justification` são feitas no controller e \
            devolvem **texto puro** (`text/plain`); corpo ausente, JSON malformado, enum inexistente e UUID inválido \
            devolvem o JSON padrão de erro.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status atualizado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UpdateFindingStatusResponse.class),
                examples = {
                    @ExampleObject(name = "Marcado como RESOLVED (reteste solicitado)", value = FindingExamples.STATUS_RESPONSE_RESOLVED),
                    @ExampleObject(name = "Outro status", value = FindingExamples.STATUS_RESPONSE_OTHER)
                })),
        @ApiResponse(responseCode = "400",
            description = "Requisição inválida. Validações do controller: texto puro. Corpo malformado, enum inexistente ou UUID inválido: JSON padrão.",
            content = {
                @Content(mediaType = "text/plain", schema = @Schema(type = "string"),
                    examples = {
                        @ExampleObject(name = "status ausente", value = FindingExamples.TEXT_STATUS_REQUIRED),
                        @ExampleObject(name = "status VALIDATED", value = FindingExamples.TEXT_STATUS_VALIDATED),
                        @ExampleObject(name = "status FAILED_VALIDATION", value = FindingExamples.TEXT_STATUS_FAILED_VALIDATION),
                        @ExampleObject(name = "JUSTIFIED sem justificativa", value = FindingExamples.TEXT_JUSTIFICATION_REQUIRED)
                    }),
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                        @ExampleObject(name = "Corpo malformado ou status inexistente", value = ErrorExamples.UNREADABLE_BODY),
                        @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                    })
            }),
        @ApiResponse(responseCode = "404", description = "Finding não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Finding não encontrado", value = ErrorExamples.FINDING_NOT_FOUND))),
        @ApiResponse(responseCode = "409",
            description = "Mudança não permitida (finding já validado) ou conflito de edição (alterado por outro processo, como um scan, durante a gravação).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Finding já validado", value = ErrorExamples.FINDING_ALREADY_VALIDATED),
                    @ExampleObject(name = "Conflito de edição", value = ErrorExamples.OPTIMISTIC_LOCK)
                }))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<?> updateFindingStatus(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do finding.",
            example = "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90") UUID findingId,
        @RequestBody(description = "Novo status (e justificativa, quando `JUSTIFIED`).", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UpdateFindingStatusRequest.class),
                examples = {
                    @ExampleObject(name = "Marcar como resolvido", value = FindingExamples.STATUS_REQUEST_RESOLVED),
                    @ExampleObject(name = "Aceitar o risco (JUSTIFIED)", value = FindingExamples.STATUS_REQUEST_JUSTIFIED)
                }))
        UpdateFindingStatusRequest request,
        @Parameter(hidden = true) Authentication authentication);

    @Operation(
        operationId = "updateFindingAssignee",
        summary = "Definir o responsável por um finding",
        description = """
            Atribui o finding a um usuário (`assigneeId`).

            - **Acesso:** `ADM` ou `DEV`.
            - O `assigneeId` é gravado como informado (a existência do usuário não é verificada).
            - O erro 400 de `assigneeId` ausente é devolvido em **texto puro** (`text/plain`).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Responsável atualizado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UpdateFindingAssigneeResponse.class),
                examples = @ExampleObject(name = "Responsável atualizado", value = FindingExamples.ASSIGNEE_RESPONSE))),
        @ApiResponse(responseCode = "400",
            description = "`assigneeId` ausente (texto puro), corpo malformado ou UUID inválido (JSON padrão).",
            content = {
                @Content(mediaType = "text/plain", schema = @Schema(type = "string"),
                    examples = @ExampleObject(name = "assigneeId ausente", value = FindingExamples.TEXT_ASSIGNEE_REQUIRED)),
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                        @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY),
                        @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                    })
            }),
        @ApiResponse(responseCode = "404", description = "Finding não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Finding não encontrado", value = ErrorExamples.FINDING_NOT_FOUND))),
        @ApiResponse(responseCode = "409", description = "Conflito de edição: o finding foi alterado por outro processo durante a gravação.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Conflito de edição", value = ErrorExamples.OPTIMISTIC_LOCK)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<?> updateFindingAssignee(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do finding.",
            example = "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90") UUID findingId,
        @RequestBody(description = "Id do usuário responsável.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UpdateFindingAssigneeRequest.class),
                examples = @ExampleObject(name = "Responsável", value = FindingExamples.ASSIGNEE_REQUEST)))
        UpdateFindingAssigneeRequest request);
}
