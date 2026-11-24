package br.uva.tcc.sentry.docs.example;

/** Exemplos de corpo de erro (formato do {@code GlobalExceptionHandler} e dos filtros de segurança). */
public final class ErrorExamples {

    public static final String UNAUTHENTICATED = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 401,
              "error": "Não autenticado",
              "message": "Autenticação necessária: token ausente, inválido ou expirado."
            }
            """;

    public static final String FORBIDDEN = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 403,
              "error": "Acesso negado",
              "message": "Seu perfil não tem permissão para realizar esta ação."
            }
            """;

    public static final String INVALID_PARAMETER = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Parâmetro inválido",
              "message": "O valor informado para 'id' é inválido."
            }
            """;

    public static final String INVALID_PARAMETER_QUERY = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Parâmetro inválido",
              "message": "O valor informado para 'severity' é inválido."
            }
            """;

    public static final String UNREADABLE_BODY = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Requisição inválida",
              "message": "O corpo da requisição está ausente ou malformado."
            }
            """;

    public static final String INVALID_CREDENTIALS = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 401,
              "error": "Credenciais inválidas",
              "message": "E-mail ou senha inválidos."
            }
            """;

    public static final String INVALID_PASSWORD_SHORT = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Senha inválida",
              "message": "A senha deve ter pelo menos 8 caracteres."
            }
            """;

    public static final String INVALID_PASSWORD_LONG = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Senha inválida",
              "message": "A senha é longa demais (máximo de 72 bytes)."
            }
            """;

    public static final String INVALID_PASSWORD_CURRENT = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Senha inválida",
              "message": "A senha atual está incorreta."
            }
            """;

    public static final String INVALID_PASSWORD_SAME = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Senha inválida",
              "message": "A nova senha deve ser diferente da atual."
            }
            """;

    public static final String EMAIL_IN_USE = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 409,
              "error": "E-mail já cadastrado",
              "message": "Já existe um usuário com o e-mail 'maria@sentry.com'."
            }
            """;

    public static final String LAST_ADMIN_DEMOTE = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 409,
              "error": "Operação não permitida",
              "message": "Não é possível rebaixar ou desativar o único administrador ativo do sistema."
            }
            """;

    public static final String LAST_ADMIN_DELETE = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 409,
              "error": "Operação não permitida",
              "message": "Não é possível excluir o único administrador ativo do sistema."
            }
            """;

    public static final String USER_NOT_FOUND = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 404,
              "error": "Recurso não encontrado",
              "message": "Usuário não encontrado."
            }
            """;

    public static final String ASSET_NOT_FOUND = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 404,
              "error": "Recurso não encontrado",
              "message": "Asset não encontrado"
            }
            """;

    public static final String FINDING_NOT_FOUND = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 404,
              "error": "Recurso não encontrado",
              "message": "Finding não encontrado."
            }
            """;

    public static final String SCAN_NOT_FOUND = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 404,
              "error": "Recurso não encontrado",
              "message": "Scan não encontrado."
            }
            """;

    public static final String SCAN_GROUP_NOT_FOUND = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 404,
              "error": "Recurso não encontrado",
              "message": "Grupo de scan não encontrado."
            }
            """;

    public static final String FINDING_ALREADY_VALIDATED = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 409,
              "error": "Mudança de status não permitida",
              "message": "Finding já validado (fechado) não pode ser alterado manualmente. Se o problema voltar, o próximo scan o reabre."
            }
            """;

    public static final String OPTIMISTIC_LOCK = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 409,
              "error": "Conflito de edição",
              "message": "O finding foi alterado por outro processo. Recarregue o board e tente novamente."
            }
            """;

    public static final String UNAUTHORIZED_TARGET = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Alvos não autorizados ou possivelmente maliciosos",
              "message": "Entrada inválida em 'targets': 127.0.0.1--script=x. Apenas IPv4, IPv6 ou notação CIDR (IP/prefixo) são aceitos."
            }
            """;

    public static final String INVALID_REPORT_FILTER_ORDER = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Filtro de relatório inválido",
              "message": "A data inicial deve ser anterior ou igual à data final."
            }
            """;

    public static final String INVALID_REPORT_FILTER_RANGE = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Filtro de relatório inválido",
              "message": "O período máximo é de 366 dias (informado: 400 dias)."
            }
            """;

    public static final String INTERNAL_ERROR = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 500,
              "error": "Internal Server Error",
              "path": "/policy"
            }
            """;

    public static final String VALIDATION_LOGIN = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Dados inválidos",
              "message": "email: O e-mail é obrigatório.; password: A senha é obrigatória."
            }
            """;

    public static final String VALIDATION_CREATE_USER = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Dados inválidos",
              "message": "name: O nome é obrigatório.; email: E-mail inválido."
            }
            """;

    public static final String VALIDATION_UPDATE_USER = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Dados inválidos",
              "message": "role: O perfil (role) é obrigatório.; active: O campo 'active' é obrigatório."
            }
            """;

    public static final String VALIDATION_PASSWORD = """
            {
              "timestamp": "2026-11-22T14:30:00.123456Z",
              "status": 400,
              "error": "Dados inválidos",
              "message": "newPassword: A nova senha é obrigatória."
            }
            """;

    private ErrorExamples() {
    }
}
