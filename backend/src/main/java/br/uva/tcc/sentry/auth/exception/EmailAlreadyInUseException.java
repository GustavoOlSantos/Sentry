package br.uva.tcc.sentry.auth.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("Já existe um usuário com o e-mail '" + email + "'.");
    }
}
