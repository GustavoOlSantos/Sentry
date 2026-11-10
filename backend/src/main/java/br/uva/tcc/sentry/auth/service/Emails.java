package br.uva.tcc.sentry.auth.service;

import java.util.Locale;

final class Emails {

    private Emails() {
    }

    /** Forma canônica gravada no banco e usada nas buscas: sem espaços nas pontas e em minúsculas. */
    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
