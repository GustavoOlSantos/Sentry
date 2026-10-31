package br.uva.tcc.sentry.finding.checker;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.shared.domain.Severity;

/**
 * Faz um handshake TLS nas portas tipicamente HTTPS (443/8443) e sinaliza
 * certificados já expirados ou perto de expirar.
 */
@Component
public class ExpiredCertificateCheck implements ConfigCheck {

    private static final Logger log = LoggerFactory.getLogger(ExpiredCertificateCheck.class);
    private final FindingFactory findingFactory;

    private static final Set<Integer> TLS_PORTS = Set.of(443, 8443);
    private static final long EXPIRING_SOON_THRESHOLD_DAYS = 30;
    private static final int DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 5000;

    private static final String EXPIRED_CODE = "EXPIRED_SSL_CERT";
    private static final String EXPIRED_TITLE = "Certificado SSL/TLS expirado";
    private static final String EXPIRED_DESCRIPTION =
            "Certificado SSL/TLS expirado. Conexões podem ser rejeitadas por clientes e navegadores, "
                    + "além do risco de segurança.";

    private static final String EXPIRING_SOON_CODE = "SSL_CERT_EXPIRING_SOON";
    private static final String EXPIRING_SOON_TITLE = "Certificado SSL/TLS perto de expirar";
    private static final String EXPIRING_SOON_DESCRIPTION =
            "Certificado SSL/TLS perto de expirar. Renovar com antecedência pra evitar interrupção.";

    private final int handshakeTimeoutMillis;

    public ExpiredCertificateCheck() {
        this(DEFAULT_HANDSHAKE_TIMEOUT_MILLIS, new FindingFactory());
    }

    public ExpiredCertificateCheck(int handshakeTimeoutMillis, FindingFactory findingFactory) {
        this.handshakeTimeoutMillis = handshakeTimeoutMillis;
        this.findingFactory = findingFactory;
    }

    @Override
    public List<Finding> check(DiscoveredHost host) {
        List<Finding> findings = new ArrayList<>();

        for (Service service : host.services()) {
            if (TLS_PORTS.contains(service.getPort())) {
                checkCertificate(host, service).ifPresent(findings::add);
            }
        }

        return findings;
    }

    private Optional<Finding> checkCertificate(DiscoveredHost host, Service service) {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
            socket.connect(
                new InetSocketAddress(host.asset().getIpAddress(), service.getPort()),
                handshakeTimeoutMillis
            );

            socket.setSoTimeout(handshakeTimeoutMillis);
            socket.startHandshake();

            SSLSession session = socket.getSession();
            Certificate[] certs = session.getPeerCertificates();

            if (certs.length == 0 || !(certs[0] instanceof X509Certificate x509)) {
                return Optional.empty();
            }

            return buildFindingIfNeeded(host, service, x509);

        } catch (Exception e) {
            log.debug(
                "Não foi possível checar certificado SSL em {}:{} ({})",
                host.asset().getIpAddress(),
                service.getPort(),
                e.getMessage()
            );

            return Optional.empty();
        }
    }

    private Optional<Finding> buildFindingIfNeeded(DiscoveredHost host, Service service, X509Certificate certificate) {
        Date notAfter = certificate.getNotAfter();
        Date now = new Date();

        if (notAfter.before(now)) {
            String evidence = "Certificado de " + host.asset().getIpAddress() + ":" + service.getPort()
                    + " expirado desde " + notAfter + ".";

            return Optional.of(findingFactory.fromPolicyViolation(
                host, service, EXPIRED_CODE, EXPIRED_TITLE, EXPIRED_DESCRIPTION,
                Severity.HIGH, evidence, null
            ));
        }

        long daysLeft = (notAfter.getTime() - now.getTime()) / (1000L * 60 * 60 * 24);

        if (daysLeft <= EXPIRING_SOON_THRESHOLD_DAYS) {
            String evidence = "Certificado de " + host.asset().getIpAddress() + ":" + service.getPort()
                    + " expira em " + daysLeft + " dia(s) (" + notAfter + ").";

            return Optional.of(findingFactory.fromPolicyViolation(
                host, service, EXPIRING_SOON_CODE, EXPIRING_SOON_TITLE, EXPIRING_SOON_DESCRIPTION,
                Severity.MEDIUM, evidence, null
            ));
        }

        return Optional.empty();
    }
}
