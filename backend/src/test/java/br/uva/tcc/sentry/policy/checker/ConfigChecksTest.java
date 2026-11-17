package br.uva.tcc.sentry.policy.checker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.shared.domain.Severity;
class ConfigChecksTest {
 private Asset asset; private FindingFactory findingFactory;
 @BeforeEach void setUp(){asset=new Asset();asset.setId(UUID.randomUUID());asset.setIpAddress("10.0.0.10");asset.setStatus(AssetStatus.UP);findingFactory=new FindingFactory();}
 @Test void exposedSshCheck_reportsOpenSshPort(){Service ssh=service(22,"tcp","open","ssh");List<Finding> findings=new ExposedSshCheck(findingFactory).check(new DiscoveredHost(asset,List.of(ssh)));assertEquals(1,findings.size());assertEquals("SSH_EXPOSED",findings.get(0).getCode());assertEquals(Severity.MEDIUM,findings.get(0).getSeverity());assertEquals(FindingType.POLICY_VIOLATION,findings.get(0).getType());}
 @Test void exposedSshCheck_ignoresClosedSshAndOtherPorts(){Service closedSsh=service(22,"tcp","closed","ssh");Service http=service(80,"tcp","open","http");List<Finding> findings=new ExposedSshCheck(findingFactory).check(new DiscoveredHost(asset,List.of(closedSsh,http)));assertTrue(findings.isEmpty());}
 @Test void exposedDatabasePortCheck_reportsKnownDatabasePorts(){Service mysql=service(3306,"tcp","open","mysql");Service redis=service(6379,"tcp","open","redis");List<Finding> findings=new ExposedDatabasePortCheck(findingFactory).check(new DiscoveredHost(asset,List.of(mysql,redis)));assertEquals(2,findings.size());assertEquals("EXPOSED_DATABASE_PORT",findings.get(0).getCode());assertEquals(Severity.HIGH,findings.get(0).getSeverity());assertEquals("EXPOSED_DATABASE_PORT",findings.get(1).getCode());}
 @Test void exposedDatabasePortCheck_ignoresNonSensitivePorts(){Service http=service(80,"tcp","open","http");Service random=service(8080,"tcp","open","unknown");List<Finding> findings=new ExposedDatabasePortCheck(findingFactory).check(new DiscoveredHost(asset,List.of(http,random)));assertTrue(findings.isEmpty());}
 private Service service(int port,String protocol,String state,String name){return new Service(port,protocol,state,name,null,null,null);}
}
