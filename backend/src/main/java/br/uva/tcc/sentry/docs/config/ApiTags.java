package br.uva.tcc.sentry.docs.config;

/**
 * Nomes dos grupos (tags) do Swagger e do esquema de segurança. Ficam em constantes
 * porque o mesmo texto é usado nas interfaces de {@code docs.api} e no {@link OpenApiConfig}.
 */
public final class ApiTags {

    public static final String AUTH = "Autenticação";
    public static final String USERS = "Usuários";
    public static final String ASSETS = "Assets";
    public static final String FINDINGS = "Findings";
    public static final String SCANS = "Scans";
    public static final String SCAN_GROUPS = "Grupos de scan";
    public static final String POLICIES = "Políticas";
    public static final String POLICY_RULES = "Regras de política";
    public static final String REPORTS = "Relatórios";

    /** Nome do esquema de segurança (JWT Bearer) declarado em {@link OpenApiConfig}. */
    public static final String BEARER_AUTH = "bearerAuth";

    private ApiTags() {
    }
}
