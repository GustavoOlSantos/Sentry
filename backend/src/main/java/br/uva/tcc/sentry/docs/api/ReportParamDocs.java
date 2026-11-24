package br.uva.tcc.sentry.docs.api;

/** Textos de parâmetros repetidos nos endpoints de relatório (visível só dentro do pacote {@code docs.api}). */
final class ReportParamDocs {

    static final String FROM = "Data inicial do período (inclusive), formato `yyyy-MM-dd`. "
            + "Padrão: 29 dias antes da data final (janela de 30 dias).";
    static final String TO = "Data final do período (inclusive), formato `yyyy-MM-dd`. Padrão: hoje. "
            + "O período não pode passar de 366 dias.";
    static final String ASSET_ID = "Restringe o relatório a um asset (UUID).";
    static final String SCAN_ID = "Restringe o relatório a um scan (UUID).";
    static final String SCAN_GROUP_ID = "Restringe o relatório a um grupo de scan (UUID).";
    static final String SEVERITY = "Filtra por severidade: `INFO`, `NONE`, `LOW`, `MEDIUM`, `HIGH` ou `CRITICAL`.";
    static final String TYPE = "Filtra por tipo de finding: `VULNERABILITY` ou `POLICY_VIOLATION`.";
    static final String STATUS = "Filtra por status do finding: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `VALIDATED`, `JUSTIFIED` ou `FAILED_VALIDATION`.";
    static final String POLICY_ID = "Restringe o relatório a uma política (id).";

    static final String PERIOD_NOTE = """
            - **Acesso:** `ADM`, `DEV` ou `USER` (leitura).
            - **Período:** sem `from`/`to`, usa os últimos 30 dias (até hoje). `from` não pode ser depois de `to` e \
            o intervalo máximo é de 366 dias; caso contrário, retorna 400.
            - Todos os filtros são opcionais e combinados com **E**.""";

    private ReportParamDocs() {
    }
}
