package br.uva.tcc.sentry.docs.example;

/** Exemplos dos endpoints de assets. */
public final class AssetExamples {

    public static final String ASSET_LIST = """
            [
              {
                "id": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                "ipAddress": "192.168.0.10",
                "hostname": "srv-web-01",
                "macAddress": "00:1A:2B:3C:4D:5E",
                "operatingSystem": "Ubuntu 22.04",
                "status": "UNHEALTHY",
                "firstSeenAt": "2026-11-05T08:00:00Z",
                "lastSeenAt": "2026-11-22T13:40:00Z",
                "services": [
                  {
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
                ],
                "findings": [
                  {
                    "id": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
                    "scanId": "b7a6c5d4-3e2f-4a1b-9c8d-7e6f5a4b3c2d",
                    "serviceId": "a1d4f0c2-7b9e-4c3a-8f21-5e6d7c8b9a01",
                    "vulnerabilityId": 15,
                    "policyRuleId": null,
                    "type": "VULNERABILITY",
                    "severity": "HIGH",
                    "status": "OPEN",
                    "title": "CVE-2023-38408 em OpenSSH 8.9p1",
                    "description": "Execução remota de código via ssh-agent encaminhado.",
                    "detectedAt": "2026-11-05T08:05:00Z",
                    "resolvedAt": null,
                    "evidence": "22/tcp open ssh OpenSSH 8.9p1"
                  }
                ]
              }
            ]
            """;

    public static final String ASSET = """
            {
              "id": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
              "ipAddress": "192.168.0.10",
              "hostname": "srv-web-01",
              "macAddress": "00:1A:2B:3C:4D:5E",
              "operatingSystem": "Ubuntu 22.04",
              "status": "UNHEALTHY",
              "firstSeenAt": "2026-11-05T08:00:00Z",
              "lastSeenAt": "2026-11-22T13:40:00Z",
              "services": [
                {
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
              ],
              "findings": []
            }
            """;

    public static final String ASSET_SUMMARY_LIST = """
            [
              {
                "id": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                "ipAddress": "192.168.0.10",
                "hostname": "srv-web-01",
                "macAddress": "00:1A:2B:3C:4D:5E",
                "operatingSystem": "Ubuntu 22.04",
                "status": "UNHEALTHY",
                "firstSeenAt": "2026-11-05T08:00:00Z",
                "lastSeenAt": "2026-11-22T13:40:00Z",
                "servicesCount": 4,
                "findingsCount": 3
              }
            ]
            """;

    public static final String ASSET_EDIT_REQUEST = """
            {
              "id": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
              "ipAddress": "192.168.0.10",
              "hostname": "srv-web-01.interno",
              "macAddress": "00:1A:2B:3C:4D:5E",
              "operatingSystem": "Ubuntu 22.04",
              "status": "UP",
              "firstSeenAt": "2026-11-05T08:00:00Z",
              "lastSeenAt": "2026-11-22T13:40:00Z"
            }
            """;

    private AssetExamples() {
    }
}
