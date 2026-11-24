package br.uva.tcc.sentry.docs.example;

/** Exemplos dos endpoints de findings. */
public final class FindingExamples {

    public static final String FINDING_BOARD = """
            [
              {
                "id": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
                "code": "CVE-2023-38408",
                "type": "VULNERABILITY",
                "severity": "HIGH",
                "status": "IN_PROGRESS",
                "title": "CVE-2023-38408 em OpenSSH 8.9p1",
                "description": "Execução remota de código via ssh-agent encaminhado.",
                "detectedAt": "2026-11-05T08:05:00Z",
                "resolvedAt": null,
                "lastSeenAt": "2026-11-22T13:40:00Z",
                "validatedAt": null,
                "reopenCount": 0,
                "evidence": "22/tcp open ssh OpenSSH 8.9p1",
                "justification": null,
                "assigneeId": 2,
                "asset": {
                  "id": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                  "ipAddress": "192.168.0.10",
                  "hostname": "srv-web-01",
                  "macAddress": "00:1A:2B:3C:4D:5E",
                  "operatingSystem": "Ubuntu 22.04",
                  "status": "UNHEALTHY"
                },
                "service": {
                  "id": "a1d4f0c2-7b9e-4c3a-8f21-5e6d7c8b9a01",
                  "port": 22,
                  "state": "open",
                  "protocol": "tcp",
                  "product": "OpenSSH",
                  "serviceName": "ssh",
                  "version": "8.9p1",
                  "extraInfo": "Ubuntu Linux; protocol 2.0",
                  "cpe": "cpe:2.3:a:openbsd:openssh:8.9p1:*:*:*:*:*:*:*"
                }
              }
            ]
            """;

    public static final String STATUS_REQUEST_RESOLVED = """
            {
              "status": "RESOLVED"
            }
            """;

    public static final String STATUS_REQUEST_JUSTIFIED = """
            {
              "status": "JUSTIFIED",
              "justification": "Risco aceito: serviço acessível apenas pela VLAN de gestão."
            }
            """;

    public static final String STATUS_RESPONSE_RESOLVED = """
            {
              "findingId": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
              "previousStatus": "IN_PROGRESS",
              "newStatus": "RESOLVED",
              "message": "Status do finding atualizado com sucesso. Um reteste foi solicitado para validar a correção."
            }
            """;

    public static final String STATUS_RESPONSE_OTHER = """
            {
              "findingId": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
              "previousStatus": "OPEN",
              "newStatus": "IN_PROGRESS",
              "message": "Status do finding atualizado com sucesso."
            }
            """;

    public static final String ASSIGNEE_REQUEST = """
            {
              "assigneeId": 2
            }
            """;

    public static final String ASSIGNEE_RESPONSE = """
            {
              "assigneeId": 2,
              "findingId": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
              "message": "Assignee do finding atualizado com sucesso."
            }
            """;

    /** Corpos de texto puro devolvidos pelas validações feitas direto no controller (não são JSON). */
    public static final String TEXT_STATUS_REQUIRED = "O campo 'status' é obrigatório.";
    public static final String TEXT_STATUS_VALIDATED = "A alteração de status para 'Válido' é permitido apenas pela própria ferramenta.";
    public static final String TEXT_STATUS_FAILED_VALIDATION = "A alteração de status para 'Inválido' é permitido apenas pela própria ferramenta.";
    public static final String TEXT_JUSTIFICATION_REQUIRED = "O campo 'justification' é obrigatório quando o status é JUSTIFIED.";
    public static final String TEXT_ASSIGNEE_REQUIRED = "O campo 'assigneeId' é obrigatório.";

    private FindingExamples() {
    }
}
