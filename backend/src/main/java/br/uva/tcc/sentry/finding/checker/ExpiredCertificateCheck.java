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

import br.uva.tcc.sentry.finding.domain.ConfigFinding;
import br.uva.tcc.sentry.finding.domain.Host;
import br.uva.tcc.sentry.finding.domain.PortInfo;

/**
 * Faz um handshake TLS nas portas tipicamente HTTPS (443/8443) e sinaliza
 * certificados já expirados ou perto de expirar.
 */
@Component
public class ExpiredCertificateCheck implements ConfigCheck {

    private static final Logger log = LoggerFactory.getLogger(ExpiredCertificateCheck.class);
    private static final Set<Integer> TLS_PORTS = Set.of(443, 8443);
    private static final long EXPIRING_SOON_THRESHOLD_DAYS = 30;
    private static final int DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 5000;

    private final int handshakeTimeoutMillis;

    public ExpiredCertificateCheck() {
        this(DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    public ExpiredCertificateCheck(int handshakeTimeoutMillis) {
        this.handshakeTimeoutMillis = handshakeTimeoutMillis;
    }

    @Override
    public List<ConfigFinding> check(Host host) {
        List<ConfigFinding> findings = new ArrayList<>();
        for (PortInfo port : host.getPorts()) {
            if (TLS_PORTS.contains(port.getPort())) {
                checkCertificate(host, port).ifPresent(findings::add);
            }
        }
        return findings;
    }

    private Optional<ConfigFinding> checkCertificate(Host host, PortInfo port) {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
            socket.connect(new InetSocketAddress(host.getIp(), port.getPort()), handshakeTimeoutMillis);
            socket.setSoTimeout(handshakeTimeoutMillis);
            socket.startHandshake();

            SSLSession session = socket.getSession();
            Certificate[] certs = session.getPeerCertificates();
            if (certs.length == 0 || !(certs[0] instanceof X509Certificate x509)) {
                return Optional.empty();
            }

            return buildFindingIfNeeded(host, port, x509);
        } catch (Exception e) {
            // Handshake pode falhar por vários motivos (host não fala TLS de fato,
            // timeout, etc.) — não é um "finding", só não conseguimos checar.
            log.debug("Não foi possível checar certificado SSL em {}:{} ({})",
                    host.getIp(), port.getPort(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ConfigFinding> buildFindingIfNeeded(Host host, PortInfo port, X509Certificate x509) {
        Date notAfter = x509.getNotAfter();
        Date now = new Date();

        if (notAfter.before(now)) {
            return Optional.of(new ConfigFinding(
                    "EXPIRED_SSL_CERT",
                    host.getIp(),
                    port.getPort(),
                    "HIGH",
                    "Certificado SSL/TLS expirado desde " + notAfter
                            + ". Conexões podem ser rejeitadas por clientes e navegadores, além do risco de segurança."
            ));
        }

        long daysLeft = (notAfter.getTime() - now.getTime()) / (1000L * 60 * 60 * 24);
        if (daysLeft <= EXPIRING_SOON_THRESHOLD_DAYS) {
            return Optional.of(new ConfigFinding(
                    "SSL_CERT_EXPIRING_SOON",
                    host.getIp(),
                    port.getPort(),
                    "MEDIUM",
                    "Certificado SSL/TLS expira em " + daysLeft + " dias (" + notAfter + "). Renovar com antecedência."
            ));
        }

        return Optional.empty();
    }
}
