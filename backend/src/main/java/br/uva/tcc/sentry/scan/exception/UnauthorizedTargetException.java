package br.uva.tcc.sentry.scan.exception;

/**
 * Lançada quando o campo {@code targets} de uma requisição de scan contém
 * uma entrada que não corresponde a um host/IP válido (IPv4, IPv6 ou
 * notação CIDR), e que portanto pode ser uma tentativa de injetar flags
 * ou argumentos extras no comando do Nmap.
 */
public class UnauthorizedTargetException extends RuntimeException {

    public UnauthorizedTargetException(String message) {
        super(message);
    }
}
