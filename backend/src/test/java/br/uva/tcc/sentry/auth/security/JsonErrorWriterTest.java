package br.uva.tcc.sentry.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonErrorWriterTest {

    @Test
    void escape_handlesQuotesBackslashesAndControlCharacters() {
        assertEquals("a\\\"b\\\\c\\nd\\te\\u0001", JsonErrorWriter.escape("a\"b\\c\nd\te\u0001"));
    }

    @Test
    void escape_keepsAccentedCharactersAsIs() {
        assertEquals("Não autenticado", JsonErrorWriter.escape("Não autenticado"));
    }
}
