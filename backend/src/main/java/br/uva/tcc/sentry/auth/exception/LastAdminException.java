package br.uva.tcc.sentry.auth.exception;

/** A operação deixaria o sistema sem nenhum administrador ativo. */
public class LastAdminException extends RuntimeException {

    public LastAdminException(String action) {
        super("Não é possível " + action + " o único administrador ativo do sistema.");
    }
}
