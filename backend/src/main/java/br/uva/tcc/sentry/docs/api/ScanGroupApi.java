package br.uva.tcc.sentry.docs.api;

import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.docs.annotation.ApiForbidden;
import br.uva.tcc.sentry.docs.annotation.ApiInvalidParameter;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.example.ScanExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import br.uva.tcc.sentry.scan.DTO.ScanGroupDetailResponse;
import br.uva.tcc.sentry.scan.DTO.ScanGroupSummaryResponse;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
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

/** Documentação de {@code /scanGroup}. Implementada por {@code ScanExecGroupController}. */
@Tag(name = ApiTags.SCAN_GROUPS)
public interface ScanGroupApi {

    @Operation(
        operationId = "listScanGroups",
        summary = "Listar grupos de scan recentes",
        description = """
            Os grupos mais recentes (cada chamada de `POST /scan/exec` cria um), com o **status derivado** dos scans do grupo, \
            a contagem de scans por status, os alvos e as contagens de ativos e findings.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            - `status` do grupo: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `NOT_FOUND`, `CANCELLED`, `ACCEPTED` ou `PARTIAL`.
            - `durationSeconds` é `null` enquanto o grupo não terminou.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Grupos recentes.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ScanGroupSummaryResponse.class)),
                examples = @ExampleObject(name = "Grupos", value = ScanExamples.SCAN_GROUP_SUMMARY_LIST))))
    @ApiUnauthorized
    List<ScanGroupSummaryResponse> getAllScansGroups();

    @Operation(
        operationId = "getScanGroupById",
        summary = "Detalhar um grupo de scan",
        description = """
            Um grupo (resumo) e a lista de scans individuais dele.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalhes do grupo.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanGroupDetailResponse.class),
                examples = @ExampleObject(name = "Grupo", value = ScanExamples.SCAN_GROUP_DETAIL))),
        @ApiResponse(responseCode = "404", description = "Grupo de scan não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Grupo não encontrado", value = ErrorExamples.SCAN_GROUP_NOT_FOUND)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    ScanGroupDetailResponse getScanGroupByID(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do grupo de scan.",
            example = "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0") UUID id);

    @Operation(
        operationId = "deleteScanGroup",
        summary = "Excluir um grupo de scan",
        description = """
            Exclui o grupo (e, em cascata, os scans dele).

            - **Acesso:** somente `ADM`.
            - A operação é idempotente: um `id` inexistente **não** gera 404 (retorna 200 do mesmo jeito).
            - Se houver registros vinculados que impeçam a exclusão no banco, a violação de integridade não é tratada \
            pela aplicação e resulta em erro **500**.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grupo excluído (sem corpo)."),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo violação de integridade no banco.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    void deleteById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do grupo de scan.",
            example = "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0") UUID id);

    @Operation(
        operationId = "updateScanGroup",
        summary = "Editar um grupo de scan",
        description = """
            Grava o grupo enviado no corpo (a entidade é persistida como veio, sem regras de negócio). \
            Endpoint de manutenção: os grupos normalmente são criados e atualizados pelo processamento da fila.

            - **Acesso:** somente `ADM`.
            - **Atenção:** o registro alterado é o identificado pelo `id` **do corpo**; o `{id}` da URL não é aplicado ao corpo.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grupo gravado (devolve a entidade persistida).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanExecutionGroup.class))),
        @ApiResponse(responseCode = "400", description = "Corpo ausente/malformado, valor de enum inexistente ou `id` com formato inválido.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY),
                    @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                })),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo violação de integridade no banco.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ScanExecutionGroup editById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do grupo de scan.",
            example = "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0") UUID id,
        @RequestBody(description = "Grupo com os dados atualizados.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanExecutionGroup.class),
                examples = @ExampleObject(name = "Edição", value = ScanExamples.SCAN_GROUP_EDIT_REQUEST)))
        ScanExecutionGroup scan);
}
