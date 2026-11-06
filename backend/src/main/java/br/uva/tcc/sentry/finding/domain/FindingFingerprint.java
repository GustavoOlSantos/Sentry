package br.uva.tcc.sentry.finding.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.domain.Service;

/**
 * Identidade estável de um {@link Finding}: o mesmo problema, no mesmo
 * serviço do mesmo asset, gera sempre o mesmo fingerprint — é isso que
 * permite reconhecer, no scan seguinte, um finding que já existe.
 *
 * Usa {@code asset + porta/protocolo + tipo + código} e propositalmente NÃO
 * usa a versão do serviço: ao atualizar o software, os CVEs antigos deixam de
 * aparecer (e são validados) e os novos viram findings novos.
 */
public final class FindingFingerprint {

    private FindingFingerprint() {
    }

    public static String of(Finding finding) {
        if (finding.getAsset() == null || finding.getAsset().getId() == null) {
            throw new IllegalStateException("O asset do finding precisa estar persistido para gerar o fingerprint.");
        }
        if (finding.getCode() == null || finding.getType() == null) {
            throw new IllegalStateException("O finding precisa de 'type' e 'code' para gerar o fingerprint.");
        }

        UUID assetId = finding.getAsset().getId();
        Service service = finding.getService();
        String serviceKey = service == null ? "-" : service.key();

        return sha256Hex(assetId + "|" + serviceKey + "|" + finding.getType() + "|" + finding.getCode());
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
