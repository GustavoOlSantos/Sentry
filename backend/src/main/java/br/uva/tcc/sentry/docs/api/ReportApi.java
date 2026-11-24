package br.uva.tcc.sentry.docs.api;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import br.uva.tcc.sentry.docs.annotation.ApiInvalidReportFilter;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.ReportExamples;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse;
import br.uva.tcc.sentry.report.DTO.RelevantFindingsResponse;
import br.uva.tcc.sentry.report.DTO.ReportSummaryResponse;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse;
import br.uva.tcc.sentry.report.EvolutionGroupBy;
import br.uva.tcc.sentry.shared.domain.Severity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Documentação de {@code /reports}. Implementada por {@code ReportController}. */
@Tag(name = ApiTags.REPORTS)
public interface ReportApi {

    @Operation(
        operationId = "getReportSummary",
        summary = "Resumo executivo",
        description = """
            Indicadores consolidados do período: total de findings, detectados/concluídos no período, contagens por status, \
            críticos e altos pendentes, ativos analisados, MTTR e compliance resumidos, além de `limitations` (ressalvas \
            sobre como os números são calculados).

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Resumo do período.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ReportSummaryResponse.class),
                examples = @ExampleObject(name = "Resumo", value = ReportExamples.SUMMARY))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    ReportSummaryResponse summary(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SEVERITY) Severity severity,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TYPE) FindingType type,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.STATUS) FindingStatus status,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.POLICY_ID, example = "1") Long policyId);

    @Operation(
        operationId = "getRiskEvolution",
        summary = "Evolução do risco",
        description = """
            Série temporal de findings detectados, resolvidos e pendentes, com a quebra por severidade.

            - `groupBy=DAY` (padrão): um ponto por dia em `points`.
            - `groupBy=SCAN`: preenche também `scans`, com um ponto por scan executado no período \
            (observados, novos, reencontrados, resolvidos pelo scan e variação em relação ao scan anterior).
            - Este relatório não aceita os filtros `status` e `policyId`.

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Evolução do risco no período.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = RiskEvolutionResponse.class),
                examples = @ExampleObject(name = "Evolução diária", value = ReportExamples.RISK_EVOLUTION))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    RiskEvolutionResponse riskEvolution(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = "Agrupamento da série: `DAY` (padrão) ou `SCAN`.", example = "DAY") EvolutionGroupBy groupBy,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SEVERITY) Severity severity,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TYPE) FindingType type);

    @Operation(
        operationId = "getFindingsDistribution",
        summary = "Distribuição dos findings",
        description = """
            Quantidade e percentual de findings por severidade, tipo, status e coluna do board \
            (`ABERTO`, `EM_ANDAMENTO`, `RESOLVIDA`, `FECHADA`), além dos ativos com mais findings.

            - Este relatório não aceita o filtro `policyId`.

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Distribuição dos findings no período.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = FindingsDistributionResponse.class),
                examples = @ExampleObject(name = "Distribuição", value = ReportExamples.DISTRIBUTION))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    FindingsDistributionResponse distribution(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SEVERITY) Severity severity,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TYPE) FindingType type,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.STATUS) FindingStatus status);

    @Operation(
        operationId = "getRelevantFindings",
        summary = "Findings mais relevantes",
        description = """
            Os findings mais relevantes do escopo, para destaque no relatório.

            - `limit`: quantidade máxima devolvida (padrão 25, mínimo 1, máximo 100 — valores fora da faixa são ajustados \
            para o limite mais próximo, sem erro). `totalInScope` informa quantos findings existem no escopo.
            - Este relatório não aceita o filtro `policyId`.

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Findings relevantes do período.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = RelevantFindingsResponse.class),
                examples = @ExampleObject(name = "Relevantes", value = ReportExamples.RELEVANT_FINDINGS))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    RelevantFindingsResponse relevantFindings(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = "Quantidade máxima de findings (1 a 100). Padrão: 25.", example = "25") int limit,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SEVERITY) Severity severity,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TYPE) FindingType type,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.STATUS) FindingStatus status);

    @Operation(
        operationId = "getMttrReport",
        summary = "MTTR (tempo médio de correção)",
        description = """
            Tempo de correção dos findings concluídos: média, mediana, mínimo e máximo (em segundos e em \
            texto legível, ex.: `2d 4h`), quantidade de concluídos e pendentes, e a quebra por severidade e por tipo.

            - Este relatório não aceita os filtros `status` e `policyId`.

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "MTTR do período.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MttrReportResponse.class),
                examples = @ExampleObject(name = "MTTR", value = ReportExamples.MTTR))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    MttrReportResponse mttr(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SEVERITY) Severity severity,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TYPE) FindingType type);

    @Operation(
        operationId = "getComplianceReport",
        summary = "Conformidade com as políticas",
        description = """
            Resultado das avaliações de regras de política: conformes, não conformes, não aplicáveis e com erro; \
            percentual de conformidade; quebra por política e por ativo; e as 10 regras mais violadas.

            - `compliancePercentage` = conformes ÷ (conformes + não conformes); `null` quando não há avaliações aplicáveis.
            - Considera apenas regras de política cadastradas (as checagens fixas do Sentry não geram avaliações). \
            Sem avaliações no período, `evaluationsAvailable` vem `false`.
            - Este relatório só aceita os filtros `policyId`, `assetId`, `scanId` e `scanGroupId` (além do período).

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Conformidade no período.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ComplianceReportResponse.class),
                examples = @ExampleObject(name = "Compliance", value = ReportExamples.COMPLIANCE))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    ComplianceReportResponse compliance(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.POLICY_ID, example = "1") Long policyId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId);

    @Operation(
        operationId = "downloadReportPdf",
        summary = "Relatório completo em PDF",
        description = """
            Gera e devolve o relatório consolidado (resumo, evolução, distribuição, MTTR, compliance e findings \
            relevantes) como **arquivo PDF** para download.

            - O nome do arquivo vem no header `Content-Disposition`: `sentry-report-<from>-to-<to>.pdf`.
            - A resposta não é cacheável (`Cache-Control: no-store`).
            - Em clientes web, o header `Content-Disposition` está exposto via CORS.

            """ + ReportParamDocs.PERIOD_NOTE,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Arquivo PDF do relatório.",
            headers = {
                @Header(name = "Content-Disposition", description = "Download do arquivo, com o nome gerado a partir do período.",
                    schema = @Schema(type = "string", example = "attachment; filename=\"sentry-report-2026-10-24-to-2026-11-22.pdf\"")),
                @Header(name = "Cache-Control", description = "Impede o cache da resposta.",
                    schema = @Schema(type = "string", example = "no-store"))
            },
            content = @Content(mediaType = "application/pdf",
                schema = @Schema(type = "string", format = "binary"))))
    @ApiInvalidReportFilter
    @ApiUnauthorized
    ResponseEntity<byte[]> pdf(
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.FROM, example = "2026-10-24") LocalDate from,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TO, example = "2026-11-22") LocalDate to,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.ASSET_ID) UUID assetId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_ID) UUID scanId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SCAN_GROUP_ID) UUID scanGroupId,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.SEVERITY) Severity severity,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.TYPE) FindingType type,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.STATUS) FindingStatus status,
        @Parameter(in = ParameterIn.QUERY, description = ReportParamDocs.POLICY_ID, example = "1") Long policyId);
}
