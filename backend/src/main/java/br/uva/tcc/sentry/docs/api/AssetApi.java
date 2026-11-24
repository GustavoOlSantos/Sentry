package br.uva.tcc.sentry.docs.api;

import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.DTO.AssetResponse;
import br.uva.tcc.sentry.Asset.DTO.AssetSummaryResponse;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.docs.annotation.ApiForbidden;
import br.uva.tcc.sentry.docs.annotation.ApiInvalidParameter;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.AssetExamples;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
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

/** Documentação de {@code /asset}. Implementada por {@code AssetController}. */
@Tag(name = ApiTags.ASSETS)
public interface AssetApi {

    @Operation(
        operationId = "listAssets",
        summary = "Listar assets (completo)",
        description = """
            Lista todos os assets com seus **serviços abertos** e **findings**.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            - Portas que já fecharam ficam no banco (os findings apontam para elas), mas **não** aparecem em `services`.
            - `status` do asset: `HEALTHY`, `UNHEALTHY`, `UP` ou `DOWN`.
            - Para uma listagem leve (só contagens), use `GET /asset/summary`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Lista de assets.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = AssetResponse.class)),
                examples = @ExampleObject(name = "Assets", value = AssetExamples.ASSET_LIST))))
    @ApiUnauthorized
    List<AssetResponse> findAll();

    @Operation(
        operationId = "listAssetSummaries",
        summary = "Listar assets (resumo)",
        description = """
            Lista os assets em formato resumido, com a **quantidade** de serviços e de findings de cada um.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Resumo dos assets.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = AssetSummaryResponse.class)),
                examples = @ExampleObject(name = "Resumo", value = AssetExamples.ASSET_SUMMARY_LIST))))
    @ApiUnauthorized
    List<AssetSummaryResponse> findAllAssetSummary();

    @Operation(
        operationId = "getAssetById",
        summary = "Buscar asset por id",
        description = """
            Devolve um asset com seus serviços abertos e findings.

            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AssetResponse.class),
                examples = @ExampleObject(name = "Asset", value = AssetExamples.ASSET))),
        @ApiResponse(responseCode = "404", description = "Asset não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Asset não encontrado", value = ErrorExamples.ASSET_NOT_FOUND)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    AssetResponse findAById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do asset.",
            example = "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11") UUID id);

    @Operation(
        operationId = "deleteAsset",
        summary = "Excluir asset",
        description = """
            Exclui o asset.

            - **Acesso:** somente `ADM`.
            - A operação é idempotente: um `id` inexistente **não** gera 404 (retorna 200 do mesmo jeito).
            - Se houver registros vinculados (serviços, findings) que impeçam a exclusão no banco, a violação de \
            integridade não é tratada pela aplicação e resulta em erro **500**.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset excluído (sem corpo)."),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo violação de integridade no banco (asset com registros vinculados).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    void deleteAById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do asset.",
            example = "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11") UUID id);

    @Operation(
        operationId = "updateAsset",
        summary = "Editar asset",
        description = """
            Grava o asset enviado no corpo (a entidade é persistida como veio, sem regras de negócio adicionais).

            - **Acesso:** `ADM` ou `DEV`.
            - **Atenção:** o registro alterado é o identificado pelo `id` **do corpo**; o `{id}` da URL não é aplicado \
            ao corpo. Envie sempre o mesmo `id` nos dois lugares — um corpo sem `id` cria um novo asset.
            - `ipAddress` é obrigatório e único; `status`: `HEALTHY`, `UNHEALTHY`, `UP` ou `DOWN`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset gravado (devolve a entidade persistida).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Asset.class),
                examples = @ExampleObject(name = "Asset gravado", value = AssetExamples.ASSET))),
        @ApiResponse(responseCode = "400", description = "Corpo ausente/malformado, valor de enum inexistente ou `id` com formato inválido.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Corpo malformado", value = ErrorExamples.UNREADABLE_BODY),
                    @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                })),
        @ApiResponse(responseCode = "500", description = "Erro inesperado, por exemplo violação de integridade no banco (`ipAddress` ausente ou duplicado).",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Erro interno", value = ErrorExamples.INTERNAL_ERROR)))
    })
    @ApiUnauthorized
    @ApiForbidden
    Asset editById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador (UUID) do asset.",
            example = "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11") UUID id,
        @RequestBody(description = "Asset com os dados atualizados.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Asset.class),
                examples = @ExampleObject(name = "Edição", value = AssetExamples.ASSET_EDIT_REQUEST)))
        Asset asset);
}
