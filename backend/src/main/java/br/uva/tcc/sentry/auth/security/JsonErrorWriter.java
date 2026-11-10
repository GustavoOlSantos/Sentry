package br.uva.tcc.sentry.auth.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escreve o corpo de erro dos filtros de segurança, que rodam ANTES do Spring MVC
 * (por isso o {@code GlobalExceptionHandler} não os alcança). Usa o mesmo formato
 * dele: timestamp, status, error e message.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escape(error) + "\","
                + "\"message\":\"" + escape(message) + "\"}");
    }

    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
