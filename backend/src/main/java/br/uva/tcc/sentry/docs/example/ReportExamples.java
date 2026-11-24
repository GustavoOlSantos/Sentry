package br.uva.tcc.sentry.docs.example;

/** Exemplos dos endpoints de relatórios. */
public final class ReportExamples {

    public static final String SUMMARY = """
            {
              "period": { "from": "2026-10-24", "to": "2026-11-22", "days": 30 },
              "scopeDescription": "Todo o ambiente",
              "totalFindings": 42,
              "detectedInPeriod": 18,
              "carriedOver": 24,
              "concludedInPeriod": 9,
              "open": 15,
              "inProgress": 8,
              "resolved": 4,
              "validated": 10,
              "resolvedOrValidated": 14,
              "acceptedRisk": 5,
              "critical": 3,
              "high": 11,
              "pendingCritical": 2,
              "pendingHigh": 7,
              "assetsAnalyzed": 25,
              "assetsWithFindings": 14,
              "mttr": { "averageSeconds": 187200, "averageLabel": "2d 4h", "completedFindings": 14 },
              "compliance": { "percentage": 87.5, "evaluations": 120 },
              "limitations": [
                "Findings JUSTIFIED são riscos aceitos e não são considerados corrigidos."
              ]
            }
            """;

    public static final String RISK_EVOLUTION = """
            {
              "period": { "from": "2026-11-20", "to": "2026-11-22", "days": 3 },
              "groupBy": "DAY",
              "scopeDescription": "Todo o ambiente",
              "points": [
                {
                  "date": "2026-11-20",
                  "detected": 4,
                  "resolved": 1,
                  "pending": 30,
                  "acceptedRisk": 5,
                  "bySeverity": { "CRITICAL": 2, "HIGH": 9, "MEDIUM": 12, "LOW": 6, "INFO": 1 }
                },
                {
                  "date": "2026-11-21",
                  "detected": 2,
                  "resolved": 3,
                  "pending": 29,
                  "acceptedRisk": 5,
                  "bySeverity": { "CRITICAL": 2, "HIGH": 9, "MEDIUM": 11, "LOW": 6, "INFO": 1 }
                }
              ],
              "scans": [],
              "limitations": []
            }
            """;

    public static final String DISTRIBUTION = """
            {
              "period": { "from": "2026-10-24", "to": "2026-11-22", "days": 30 },
              "scopeDescription": "Todo o ambiente",
              "total": 42,
              "bySeverity": [
                { "severity": "CRITICAL", "count": 3, "percentage": 7.14 },
                { "severity": "HIGH", "count": 11, "percentage": 26.19 }
              ],
              "byType": [
                { "type": "VULNERABILITY", "count": 30, "percentage": 71.43 },
                { "type": "POLICY_VIOLATION", "count": 12, "percentage": 28.57 }
              ],
              "byStatus": [
                { "status": "OPEN", "column": "ABERTO", "count": 15, "percentage": 35.71 },
                { "status": "IN_PROGRESS", "column": "EM_ANDAMENTO", "count": 8, "percentage": 19.05 }
              ],
              "byKanbanColumn": [
                { "column": "ABERTO", "count": 15, "percentage": 35.71 },
                { "column": "FECHADA", "count": 15, "percentage": 35.71 }
              ],
              "topAssets": [
                {
                  "assetId": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                  "ipAddress": "192.168.0.10",
                  "hostname": "srv-web-01",
                  "total": 9,
                  "bySeverity": { "CRITICAL": 1, "HIGH": 4, "MEDIUM": 3, "LOW": 1 },
                  "pending": 6,
                  "open": 4,
                  "validated": 2,
                  "acceptedRisk": 1
                }
              ]
            }
            """;

    public static final String RELEVANT_FINDINGS = """
            {
              "period": { "from": "2026-10-24", "to": "2026-11-22", "days": 30 },
              "limit": 25,
              "totalInScope": 42,
              "findings": [
                {
                  "id": "9c7e1b2a-4d3f-4a58-b6e0-2f1a3c5d7e90",
                  "title": "CVE-2023-38408 em OpenSSH 8.9p1",
                  "code": "CVE-2023-38408",
                  "severity": "HIGH",
                  "type": "VULNERABILITY",
                  "status": "OPEN",
                  "assetIp": "192.168.0.10",
                  "assetHostname": "srv-web-01",
                  "port": 22,
                  "protocol": "tcp",
                  "detectedAt": "2026-11-05T08:05:00"
                }
              ]
            }
            """;

    public static final String MTTR = """
            {
              "period": { "from": "2026-10-24", "to": "2026-11-22", "days": 30 },
              "scopeDescription": "Todo o ambiente",
              "averageSeconds": 187200,
              "averageHours": 52.0,
              "averageLabel": "2d 4h",
              "medianSeconds": 86400,
              "medianLabel": "1d 0h",
              "minSeconds": 3600,
              "minLabel": "1h",
              "maxSeconds": 604800,
              "maxLabel": "7d 0h",
              "completedFindings": 14,
              "pendingFindings": 23,
              "bySeverity": [
                { "severity": "CRITICAL", "averageSeconds": 43200, "averageHours": 12.0, "averageLabel": "12h", "completedCount": 2 }
              ],
              "byType": [
                { "type": "VULNERABILITY", "averageSeconds": 216000, "averageHours": 60.0, "averageLabel": "2d 12h", "completedCount": 10 }
              ]
            }
            """;

    public static final String COMPLIANCE = """
            {
              "period": { "from": "2026-10-24", "to": "2026-11-22", "days": 30 },
              "scopeDescription": "Todo o ambiente",
              "evaluationsAvailable": true,
              "totalEvaluations": 120,
              "compliant": 105,
              "nonCompliant": 12,
              "notApplicable": 2,
              "errors": 1,
              "compliancePercentage": 89.74,
              "byPolicy": [
                {
                  "policyId": 1,
                  "policyName": "Hardening de servidores",
                  "compliant": 105,
                  "nonCompliant": 12,
                  "notApplicable": 2,
                  "errors": 1,
                  "compliancePercentage": 89.74
                }
              ],
              "byAsset": [
                {
                  "assetId": "3f2b8c1e-5a7d-4e0a-9d52-1c6f0a9b7e11",
                  "ipAddress": "192.168.0.10",
                  "hostname": "srv-web-01",
                  "compliant": 8,
                  "nonCompliant": 2,
                  "notApplicable": 0,
                  "errors": 0,
                  "compliancePercentage": 80.0
                }
              ],
              "topViolations": [
                { "ruleId": 1, "ruleName": "Telnet proibido", "policyName": "Hardening de servidores", "severity": "HIGH", "affectedAssets": 4 }
              ]
            }
            """;

    private ReportExamples() {
    }
}
