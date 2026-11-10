package br.uva.tcc.sentry.auth.exception;

/** Login recusado. A mensagem é propositalmente genérica (não revela se o e-mail existe). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos.");
    }
}
