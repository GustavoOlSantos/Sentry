package br.uva.tcc.sentry.scan.scanner.nmap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
class CpeFormatConverterTest {
 private final CpeFormatConverter converter=new CpeFormatConverter();
 @Test void convertsNmapUriCpeToCpe23(){assertEquals("cpe:2.3:a:apache:http_server:2.4.29:*:*:*:*:*:*:*",converter.toCpe23("cpe:/a:apache:http_server:2.4.29"));}
 @Test void convertsCpeWithoutPrefix(){assertEquals("cpe:2.3:a:nginx:nginx:1.24.0:*:*:*:*:*:*:*",converter.toCpe23("a:nginx:nginx:1.24.0"));}
 @Test void fillsMissingFieldsWithWildcards(){assertEquals("cpe:2.3:a:vendor:product:*:*:*:*:*:*:*:*",converter.toCpe23("cpe:/a:vendor:product"));}
 @Test void nullAndBlankInputReturnsNull(){assertNull(converter.toCpe23(null));assertNull(converter.toCpe23(""));assertNull(converter.toCpe23("   "));}
}
