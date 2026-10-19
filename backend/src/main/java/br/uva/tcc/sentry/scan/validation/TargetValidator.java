package br.uva.tcc.sentry.scan.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Valida se uma string de destino de scan é um host legítimo (IPv4, IPv6
 * ou um range em notação CIDR), e nada além disso.
 *
 * Isso existe especificamente para impedir que o campo {@code targets}
 * seja usado para injetar argumentos extras no comando do Nmap (ex.:
 * {@code --script}, {@code -oG}, espaços, aspas, etc.), já que o conjunto
 * de caracteres aceito pelos padrões abaixo é restrito a dígitos, letras
 * hexadecimais (a-f), pontos, dois-pontos e barra — nada que o Nmap
 * interprete como uma flag.
 */
@Component
public class TargetValidator {

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";

    private static final Pattern IPV4 = Pattern.compile(
            "^" + OCTET + "(\\." + OCTET + "){3}$");

    private static final Pattern IPV4_CIDR = Pattern.compile(
            "^" + OCTET + "(\\." + OCTET + "){3}/(3[0-2]|[12]?\\d)$");

    // Cobre as formas mais comuns de IPv6 (completo, com "::" em qualquer
    // posição). Não cobre formas raras como endereços com escopo (%eth0)
    // ou o formato IPv4-mapeado (::ffff:a.b.c.d).
    private static final String IPV6_BODY =
            "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}" +
            "|([0-9a-fA-F]{1,4}:){1,7}:" +
            "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}" +
            "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}" +
            "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}" +
            "|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}" +
            "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}" +
            "|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})" +
            "|:((:[0-9a-fA-F]{1,4}){1,7}|:))";

    private static final Pattern IPV6 = Pattern.compile("^" + IPV6_BODY + "$");

    private static final Pattern IPV6_CIDR = Pattern.compile(
            "^" + IPV6_BODY + "/(12[0-8]|1[01]\\d|[1-9]?\\d)$");

    /**
     * @param target uma única entrada (já sem espaços), ex.: "192.168.1.1",
     *               "192.168.1.0/24", "::1" ou "2001:db8::/32".
     * @return true se bater em algum dos formatos aceitos.
     */
    public boolean isValid(String target) {
        if (target == null || target.isBlank()) {
            return false;
        }

        return IPV4.matcher(target).matches()
                || IPV4_CIDR.matcher(target).matches()
                || IPV6.matcher(target).matches()
                || IPV6_CIDR.matcher(target).matches();
    }
}
