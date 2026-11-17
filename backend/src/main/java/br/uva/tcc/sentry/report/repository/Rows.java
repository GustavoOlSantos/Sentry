package br.uva.tcc.sentry.report.repository;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

final class Rows {

    private Rows() {
    }

    static String str(Object value) {
        return value == null ? null : value.toString();
    }

    static long lng(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    static Long lngOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    static Double dbl(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal ? decimal.doubleValue() : ((Number) value).doubleValue();
    }

    static Integer intOrNull(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    static LocalDate date(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    static LocalDateTime dateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }

    static UUID uuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof byte[] bytes && bytes.length == 16) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }
}
