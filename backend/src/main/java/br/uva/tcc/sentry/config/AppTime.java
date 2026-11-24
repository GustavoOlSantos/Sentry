package br.uva.tcc.sentry.config;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

/**
 * Regra de tempo da aplicação.
 *
 * Todo {@code LocalDateTime} gravado no banco (scans, findings, usuários...) vem de {@code LocalDateTime.now()},
 * que usa o fuso da JVM, e não carrega zona. Para o valor ter sempre o mesmo significado, a JVM é fixada em UTC
 * na inicialização e o horário é <b>armazenado em UTC</b>. Quem exibe (navegador, PDF) converte para o fuso do usuário.
 */
public final class AppTime {

    /** Fuso em que os horários são gravados. */
    public static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;

    /** Fuso usado onde o servidor precisa formatar para uma pessoa (PDF); configurável em {@code sentry.time.display-zone}. */
    public static final ZoneId DEFAULT_DISPLAY_ZONE = ZoneId.of("America/Sao_Paulo");

    private AppTime() {
    }

    /** Chamar antes de subir o Spring, para que {@code LocalDateTime.now()} e o driver JDBC já usem UTC. */
    public static void pinStorageZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(STORAGE_ZONE));
    }

    /** Converte um horário gravado (UTC, sem zona) para o horário de parede no fuso informado. */
    public static LocalDateTime toZone(LocalDateTime stored, ZoneId zone) {
        return stored == null ? null : stored.atOffset(STORAGE_ZONE).atZoneSameInstant(zone).toLocalDateTime();
    }
}
