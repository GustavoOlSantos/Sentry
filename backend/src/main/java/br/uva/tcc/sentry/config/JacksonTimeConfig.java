package br.uva.tcc.sentry.config;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Faz toda data/hora ({@link LocalDateTime}) sair no JSON com a zona explícita, em UTC:
 * {@code 2026-11-22T14:30:00Z} em vez de {@code 2026-11-22T14:30:00}.
 *
 * Sem o {@code Z}, o navegador interpreta o valor como horário local e mostra a hora UTC como se fosse a do usuário.
 * Com ele, {@code new Date(valor)} converte sozinho para o fuso de quem está olhando.
 *
 * Só afeta a saída. Na entrada o Jackson já aceita o valor com {@code Z}.
 */
@Configuration
public class JacksonTimeConfig {

    @Bean
    public SimpleModule utcLocalDateTimeModule() {
        SimpleModule module = new SimpleModule("sentry-utc-local-date-time");
        module.addSerializer(LocalDateTime.class, new UtcLocalDateTimeSerializer());
        return module;
    }

    static final class UtcLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(format(value));
        }

        static String format(LocalDateTime value) {
            return value.toInstant(AppTime.STORAGE_ZONE).toString();
        }
    }
}
