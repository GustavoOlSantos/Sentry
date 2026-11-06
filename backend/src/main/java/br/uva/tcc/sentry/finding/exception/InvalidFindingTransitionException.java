package br.uva.tcc.sentry.finding.exception;

/**
 * Lançada quando uma mudança de status de um {@code Finding} não é permitida
 * pelas regras do ciclo de vida (ex.: o usuário tentando marcar como
 * VALIDATED, status que só a própria ferramenta pode atribuir).
 */
public class InvalidFindingTransitionException extends RuntimeException {

    public InvalidFindingTransitionException(String message) {
        super(message);
    }
}
