package br.uva.tcc.sentry.scan.validation;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class TargetValidatorTest {
 private final TargetValidator validator=new TargetValidator();
 @Test void acceptsValidIpv4(){assertTrue(validator.isValid("192.168.1.10"));assertTrue(validator.isValid("10.0.0.1"));assertTrue(validator.isValid("255.255.255.255"));}
 @Test void acceptsValidIpv4Cidr(){assertTrue(validator.isValid("192.168.1.0/24"));assertTrue(validator.isValid("10.0.0.0/8"));assertTrue(validator.isValid("172.16.0.0/32"));}
 @Test void acceptsValidIpv6(){assertTrue(validator.isValid("::1"));assertTrue(validator.isValid("2001:db8::1"));assertTrue(validator.isValid("2001:db8:0:0:0:0:0:1"));}
 @Test void acceptsValidIpv6Cidr(){assertTrue(validator.isValid("2001:db8::/32"));assertTrue(validator.isValid("::1/128"));}
 @Test void rejectsNullAndBlankTargets(){assertFalse(validator.isValid(null));assertFalse(validator.isValid(""));assertFalse(validator.isValid("   "));}
 @Test void rejectsInvalidIpv4(){assertFalse(validator.isValid("256.1.1.1"));assertFalse(validator.isValid("192.168.1"));assertFalse(validator.isValid("192.168.1.1.1"));}
 @Test void rejectsInvalidCidrPrefix(){assertFalse(validator.isValid("192.168.1.0/33"));assertFalse(validator.isValid("2001:db8::/129"));}
 @Test void rejectsShellAndNmapInjectionCharacters(){assertFalse(validator.isValid("192.168.1.1 --script vuln"));assertFalse(validator.isValid("192.168.1.1;rm -rf /"));assertFalse(validator.isValid("192.168.1.1 -oG output"));assertFalse(validator.isValid("\"192.168.1.1\""));}
}
