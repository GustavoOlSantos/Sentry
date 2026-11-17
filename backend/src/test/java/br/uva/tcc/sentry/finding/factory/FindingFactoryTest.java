package br.uva.tcc.sentry.finding.factory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.shared.domain.Severity;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;
class FindingFactoryTest {
 private FindingFactory factory; private Asset asset; private Service service; private DiscoveredHost host; private Scan scan;
 @BeforeEach void setUp(){factory=new FindingFactory();asset=new Asset();asset.setId(UUID.randomUUID());asset.setIpAddress("10.0.0.5");asset.setStatus(AssetStatus.UP);service=new Service(443,"tcp","open","https","nginx","1.24.0","cpe:/a:nginx:nginx:1.24.0");host=new DiscoveredHost(asset,List.of(service));scan=new Scan("10.0.0.5");}
 @Test void vulnerabilityFinding_containsCoreObservationData(){Vulnerability vulnerability=new Vulnerability("CVE-2026-1234",9.8,"Remote code execution vulnerability in the service.");Finding finding=factory.fromVulnerability(host,service,vulnerability,scan);assertEquals(asset,finding.getAsset());assertEquals(service,finding.getService());assertEquals(scan,finding.getScan());assertEquals(FindingType.VULNERABILITY,finding.getType());assertEquals(FindingStatus.OPEN,finding.getStatus());assertEquals(Severity.CRITICAL,finding.getSeverity());assertEquals("CVE-2026-1234",finding.getCode());assertNotNull(finding.getDetectedAt());assertTrue(finding.getEvidence().contains("nginx 1.24.0"));assertTrue(finding.getEvidence().contains("CPE: cpe:/a:nginx:nginx:1.24.0"));assertTrue(finding.getEvidence().contains("CVE-2026-1234"));}
 @Test void policyFinding_preservesCodeTitleDescriptionAndEvidence(){Finding finding=factory.fromPolicyViolation(host,service,"TEST_RULE","Regra de teste","Descrição da regra",Severity.HIGH,"porta 443 aberta",scan);assertEquals(FindingType.POLICY_VIOLATION,finding.getType());assertEquals("TEST_RULE",finding.getCode());assertEquals("Regra de teste",finding.getTitle());assertEquals("Descrição da regra",finding.getDescription());assertEquals("porta 443 aberta",finding.getEvidence());assertEquals(scan,finding.getScan());}
 @Test void vulnerabilityEvidenceFallsBackToServiceNameWithoutProduct(){Service serviceWithoutProduct=new Service(8080,"tcp","open","tomcat",null,null,null);Finding finding=factory.fromVulnerability(new DiscoveredHost(asset,List.of(serviceWithoutProduct)),serviceWithoutProduct,new Vulnerability("CVE-2026-0001",5.0,"Example vulnerability."),scan);assertTrue(finding.getEvidence().contains("tomcat"));assertTrue(finding.getEvidence().contains("8080/tcp"));}
}
