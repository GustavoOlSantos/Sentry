package br.uva.tcc.sentry.docs.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import br.uva.tcc.sentry.docs.annotation.ApiForbidden;
import br.uva.tcc.sentry.docs.annotation.ApiInvalidParameter;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.example.ScanExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse;
import br.uva.tcc.sentry.scan.DTO.ScanRequest;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.DTO.ScanSummaryResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
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

/** Documentação de {@code /scan}. Implementada por {@code ScanController}. */
@Tag(name = ApiTags.SCANS)
public interface ScanApi {

    @Operation(
        operationId = "listScans",
        summary = "Listar scans recentes",
        description = """
            Os scans mais recentes (de qualquer grupo), em formato resumido: alvo, status, duração e contagens de \
            ativos, findings e avisos.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            - `status`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `NOT_FOUND` ou `CANCELLED`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Scans recentes.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ScanSummaryResponse.class)),
                examples = @ExampleObject(name = "Scans", value = ScanExamples.SCAN_SUMMARY_LIST))))
    @ApiUnauthorized
    List<ScanSummaryResponse> getAllScans();

    @Operation(
        operationId = "getScanById",
        summary = "Detalhar um scan",
        description = """
            Detalhes de um scan: comando Nmap executado, avisos, ativos encontrados (com portas abertas) e os findings \
            que **este scan detectou pela primeira vez** (reencontros de findings antigos não entram).

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalhes do scan.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanDetailResponse.class),
                examples = @ExampleObject(name = "Scan", value = ScanExamples.SCAN_DETAIL))),
        @ApiResponse(responseCode = "404", description = "Scan não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Scan não encontrado", value = ErrorExamples.SCAN_NOT_FOUND)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    ScanDetailResponse getSingleScanByID(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do scan.",
            example = "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d") UUID id);

    @Operation(
        operationId = "deleteScan",
        summary = "Excluir um scan",
        description = """
            Exclui o scan.

            - **Acesso:** somente `ADM`.
            - A operação é idempotente: um `id` inexistente **não** gera 404 (retorna 200 do mesmo jeito).
            - Se houver registros vinculados que impeçam a exclusão no banco, a violação de integridade não é tratada \
            pela aplicação e resulta em erro **500**.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scan excluído (sem corpo)."),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo violação de integridade no banco.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    void deleteById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do scan.",
            example = "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d") UUID id);

    @Operation(
        operationId = "updateScan",
        summary = "Editar um scan",
        description = """
            Grava o scan enviado no corpo (a entidade é persistida como veio, sem regras de negócio). \
            Endpoint de manutenção: os scans normalmente são criados e atualizados pelo processamento da fila.

            - **Acesso:** somente `ADM`.
            - **Atenção:** o registro alterado é o identificado pelo `id` **do corpo**; o `{id}` da URL não é aplicado ao corpo.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scan gravado (devolve a entidade persistida).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Scan.class))),
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
    Scan editById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do scan.",
            example = "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d") UUID id,
        @RequestBody(description = "Scan com os dados atualizados.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Scan.class),
                examples = @ExampleObject(name = "Edição", value = ScanExamples.SCAN_EDIT_REQUEST)))
        Scan scan);

    @Operation(
        operationId = "executeScan",
        summary = "Executar scan",
        description = """
            Cria um **grupo de scans** com um scan por alvo e publica cada um na fila (RabbitMQ) para execução assíncrona. \
            A resposta indica apenas se a **publicação** foi confirmada — o andamento e o resultado são consultados em \
            `GET /scan` e `GET /scanGroup`.

            - **Acesso:** `ADM` ou `DEV`.
            - `targets`: um ou mais alvos separados por vírgula (espaços são ignorados). Cada alvo deve ser **IPv4, IPv6 ou \
            CIDR** (ex.: `192.168.0.10, 10.0.0.0/24`); nomes de host e qualquer outro texto são recusados \
            (proteção contra injeção de argumentos no Nmap). Alvos repetidos são ignorados.
            - Status da resposta: `ACCEPTED` (todas as publicações confirmadas → **202**), `PARTIAL` (algumas confirmadas → **202**) \
            ou `FAILED` (nenhuma confirmada → **503**). O detalhe de cada alvo está em `publicacoes[].status` \
            (`CONFIRMED`, `NOT_CONFIRMED` ou `FAILED`).

            **Atenção ao formato dos erros 400:** `targets` vazio devolve `{ "message": "..." }`; alvo inválido devolve o \
            JSON padrão de erro.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitação aceita: todas (`ACCEPTED`) ou parte (`PARTIAL`) das publicações foram confirmadas.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanResponse.class),
                examples = {
                    @ExampleObject(name = "Todas confirmadas (ACCEPTED)", value = ScanExamples.EXEC_ACCEPTED),
                    @ExampleObject(name = "Confirmação parcial (PARTIAL)", value = ScanExamples.EXEC_PARTIAL)
                })),
        @ApiResponse(responseCode = "400",
            description = "`targets` ausente/vazio, alvo inválido (não é IPv4, IPv6 ou CIDR) ou corpo malformado.",
            content = {
                @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                        @ExampleObject(name = "Alvo inválido", value = ErrorExamples.UNAUTHORIZED_TARGET),
                        @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY),
                        @ExampleObject(name = "targets vazio (formato { message })", value = ScanExamples.EXEC_TARGETS_REQUIRED)
                    })
            }),
        @ApiResponse(responseCode = "503",
            description = "Nenhuma solicitação foi confirmada pelo RabbitMQ (`status = FAILED`). O grupo e os scans foram criados, mas não entraram na fila; tente novamente mais tarde.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanResponse.class),
                examples = @ExampleObject(name = "Nenhuma confirmada (FAILED)", value = ScanExamples.EXEC_FAILED)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<?> executeScan(
        @RequestBody(description = "Alvos a escanear, separados por vírgula.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ScanRequest.class),
                examples = @ExampleObject(name = "Dois alvos", value = ScanExamples.EXEC_REQUEST)))
        ScanRequest request);
}
