package br.uva.tcc.sentry.docs.example;

/** Exemplos dos endpoints de scans e grupos de scan. */
public final class ScanExamples {

    public static final String SCAN_SUMMARY_LIST = """
            [
              {
                "id": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
                "groupId": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
                "target": "192.168.0.10",
                "status": "COMPLETED",
                "createdAt": "2026-11-22T13:30:00",
                "startedAt": "2026-11-22T13:30:05",
                "finishedAt": "2026-11-22T13:32:40",
                "durationSeconds": 155,
                "errorMessage": null,
                "assetsCount": 1,
                "findingsCount": 3,
                "warningsCount": 0
              }
            ]
            """;

    public static final String SCAN_DETAIL = """
            {
              "id": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
              "groupId": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
              "target": "192.168.0.10",
              "command": "nmap -sV --version-light -p- -O -oX /tmp/sentry-scan.xml 192.168.0.10",
              "status": "COMPLETED",
              "errorMessage": null,
              "warnings": [],
              "createdAt": "2026-11-22T13:30:00",
              "startedAt": "2026-11-22T13:30:05",
              "finishedAt": "2026-11-22T13:32:40",
              "durationSeconds": 155,
              "assets": [
                {
                  "id": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                  "ipAddress": "192.168.0.10",
                  "hostname": "srv-web-01",
                  "operatingSystem": "Ubuntu 22.04",
                  "status": "UP",
                  "discoveredAt": "2026-11-22T13:32:30",
                  "openPorts": [
                    { "port": 22, "protocol": "tcp", "service": "ssh", "product": "OpenSSH", "version": "8.9p1" }
                  ]
                }
              ],
              "findings": [
                {
                  "id": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
                  "type": "VULNERABILITY",
                  "code": "CVE-2023-38408",
                  "title": "CVE-2023-38408 em OpenSSH 8.9p1",
                  "severity": "HIGH",
                  "status": "OPEN",
                  "assetId": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                  "assetIp": "192.168.0.10",
                  "port": 22,
                  "protocol": "tcp"
                }
              ]
            }
            """;

    public static final String SCAN_EDIT_REQUEST = """
            {
              "id": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
              "target": "192.168.0.10",
              "status": "CANCELLED"
            }
            """;

    public static final String EXEC_REQUEST = """
            {
              "targets": "192.168.0.10, 10.0.0.0/24"
            }
            """;

    public static final String EXEC_ACCEPTED = """
            {
              "message": "Solicitações de scan aceitas para processamento.",
              "groupId": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
              "status": "ACCEPTED",
              "totalTargets": 2,
              "publicacoes": [
                {
                  "target": "192.168.0.10",
                  "scanId": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
                  "status": "CONFIRMED",
                  "message": "Publicação confirmada pelo RabbitMQ."
                },
                {
                  "target": "10.0.0.0/24",
                  "scanId": "c8b7d6e5-4f30-4b2c-8d9e-8f7a6b5c4d3e",
                  "status": "CONFIRMED",
                  "message": "Publicação confirmada pelo RabbitMQ."
                }
              ]
            }
            """;

    public static final String EXEC_PARTIAL = """
            {
              "message": "Nem todas as solicitações de scan foram confirmadas.",
              "groupId": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
              "status": "PARTIAL",
              "totalTargets": 2,
              "publicacoes": [
                {
                  "target": "192.168.0.10",
                  "scanId": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
                  "status": "CONFIRMED",
                  "message": "Publicação confirmada pelo RabbitMQ."
                },
                {
                  "target": "10.0.0.0/24",
                  "scanId": "c8b7d6e5-4f30-4b2c-8d9e-8f7a6b5c4d3e",
                  "status": "NOT_CONFIRMED",
                  "message": "Publicação não confirmada pelo broker."
                }
              ]
            }
            """;

    public static final String EXEC_FAILED = """
            {
              "message": "Nenhuma solicitação de scan foi confirmada pelo RabbitMQ.",
              "groupId": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
              "status": "FAILED",
              "totalTargets": 1,
              "publicacoes": [
                {
                  "target": "192.168.0.10",
                  "scanId": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
                  "status": "FAILED",
                  "message": "Connection refused"
                }
              ]
            }
            """;

    public static final String EXEC_TARGETS_REQUIRED = """
            {
              "message": "O campo 'targets' é obrigatório e não pode estar vazio."
            }
            """;

    public static final String SCAN_GROUP_SUMMARY_LIST = """
            [
              {
                "id": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
                "status": "COMPLETED",
                "createdAt": "2026-11-22T13:30:00",
                "startedAt": "2026-11-22T13:30:05",
                "finishedAt": "2026-11-22T13:35:12",
                "durationSeconds": 307,
                "errorMessage": null,
                "totalScans": 2,
                "scansByStatus": { "COMPLETED": 2 },
                "targets": ["192.168.0.10", "10.0.0.0/24"],
                "assetsCount": 12,
                "findingsCount": 7
              }
            ]
            """;

    public static final String SCAN_GROUP_DETAIL = """
            {
              "group": {
                "id": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
                "status": "COMPLETED",
                "createdAt": "2026-11-22T13:30:00",
                "startedAt": "2026-11-22T13:30:05",
                "finishedAt": "2026-11-22T13:35:12",
                "durationSeconds": 307,
                "errorMessage": null,
                "totalScans": 2,
                "scansByStatus": { "COMPLETED": 2 },
                "targets": ["192.168.0.10", "10.0.0.0/24"],
                "assetsCount": 12,
                "findingsCount": 7
              },
              "scans": [
                {
                  "id": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
                  "groupId": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0",
                  "target": "192.168.0.10",
                  "status": "COMPLETED",
                  "createdAt": "2026-11-22T13:30:00",
                  "startedAt": "2026-11-22T13:30:05",
                  "finishedAt": "2026-11-22T13:32:40",
                  "durationSeconds": 155,
                  "errorMessage": null,
                  "assetsCount": 1,
                  "findingsCount": 3,
                  "warningsCount": 0
                }
              ]
            }
            """;

    public static final String SCAN_GROUP_EDIT_REQUEST = """
            {
              "id": "0e1d2c3b-4a59-4687-8796-a5b4c3d2e1f0"
            }
            """;

    private ScanExamples() {
    }
}
