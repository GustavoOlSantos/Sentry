package br.uva.tcc.sentry.docs.example;

/** Exemplos dos endpoints de políticas e regras de política. */
public final class PolicyExamples {

    public static final String POLICY_REQUEST = """
            {
              "name": "Hardening de servidores",
              "description": "Serviços e portas que não devem estar expostos em servidores de produção.",
              "status": "ACTIVE"
            }
            """;

    public static final String POLICY = """
            {
              "id": 1,
              "name": "Hardening de servidores",
              "description": "Serviços e portas que não devem estar expostos em servidores de produção.",
              "status": "ACTIVE",
              "createdAt": "2026-11-10T10:00:00",
              "updatedAt": "2026-11-10T10:00:00"
            }
            """;

    public static final String POLICY_LIST = """
            [
              {
                "id": 1,
                "name": "Hardening de servidores",
                "description": "Serviços e portas que não devem estar expostos em servidores de produção.",
                "status": "ACTIVE",
                "createdAt": "2026-11-10T10:00:00",
                "updatedAt": "2026-11-10T10:00:00"
              }
            ]
            """;

    public static final String RULE_REQUEST_FORBIDDEN_SERVICE = """
            {
              "policy": { "id": 1 },
              "name": "Telnet proibido",
              "description": "Nenhum host pode rodar telnet.",
              "type": "FORBIDDEN_SERVICE",
              "expectedValue": "telnet",
              "severity": "HIGH",
              "enabled": true
            }
            """;

    public static final String RULE_REQUEST_PORT_RESTRICTION = """
            {
              "policy": { "id": 1 },
              "name": "Porta 3389 (RDP) fechada",
              "description": "A porta 3389/tcp não pode estar aberta.",
              "type": "PORT_RESTRICTION",
              "condition": "tcp",
              "expectedValue": "3389",
              "severity": "CRITICAL",
              "enabled": true
            }
            """;

    public static final String RULE = """
            {
              "id": 1,
              "policy": {
                "id": 1,
                "name": "Hardening de servidores",
                "description": "Serviços e portas que não devem estar expostos em servidores de produção.",
                "status": "ACTIVE",
                "createdAt": "2026-11-10T10:00:00",
                "updatedAt": "2026-11-10T10:00:00"
              },
              "name": "Telnet proibido",
              "description": "Nenhum host pode rodar telnet.",
              "type": "FORBIDDEN_SERVICE",
              "condition": null,
              "expectedValue": "telnet",
              "severity": "HIGH",
              "enabled": true
            }
            """;

    private PolicyExamples() {
    }
}
