package br.uva.tcc.sentry.scan.scanner.nmap;

import org.springframework.stereotype.Component;

/**
 * O Nmap reporta CPE no formato "URI" (2.2), ex: {@code cpe:/a:apache:http_server:2.4.29}.
 * A API 2.0 do NVD exige o formato "formatted string" (2.3), ex:
 * {@code cpe:2.3:a:apache:http_server:2.4.29:*:*:*:*:*:*:*}.
 *
 * Esta classe faz essa conversão. Não cobre 100% dos casos exóticos da
 * especificação CPE, mas cobre o formato que o Nmap efetivamente emite —
 * por isso fica dentro do pacote {@code scan.scanner.nmap} (é uma
 * particularidade da leitura do Nmap, não um utilitário genérico).
 */
@Component
public class CpeFormatConverter {

    // part, vendor, product, version, update, edition, language, sw_edition, target_sw, target_hw, other
    private static final int CPE23_FIELD_COUNT = 11;

    public String toCpe23(String nmapCpe) {
        if (nmapCpe == null || nmapCpe.isBlank()) {
            return null;
        }

        String body = nmapCpe.startsWith("cpe:/") ? nmapCpe.substring(5) : nmapCpe;
        String[] parts = body.split(":");

        StringBuilder cpe23 = new StringBuilder("cpe:2.3");
        for (int i = 0; i < CPE23_FIELD_COUNT; i++) {
            cpe23.append(':');
            if (i < parts.length && !parts[i].isBlank()) {
                cpe23.append(parts[i]);
            } else {
                cpe23.append('*');
            }
        }
        return cpe23.toString();
    }
}
