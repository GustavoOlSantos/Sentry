package br.uva.tcc.sentry.scan.scanner.nmap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.uva.tcc.sentry.scan.exception.PermanentScanException;
class NmapCommandBuilderTest {
 private final NmapCommandBuilder builder=new NmapCommandBuilder();
 @Test void buildsExpectedNmapCommand(){assertEquals(List.of("nmap","-sV","--version-light","-O","-oX","/tmp/scan.xml","192.168.1.10"),builder.build("192.168.1.10","/tmp/scan.xml"));}
 @Test void keepsTargetAsSingleCommandArgument(){List<String> command=builder.build("2001:db8::1","result.xml");assertEquals(7,command.size());assertEquals("2001:db8::1",command.get(6));}
 @Test void rejectsBlankTarget(){assertThrows(PermanentScanException.class,()->builder.build(null,"result.xml"));assertThrows(PermanentScanException.class,()->builder.build("   ","result.xml"));}
}
