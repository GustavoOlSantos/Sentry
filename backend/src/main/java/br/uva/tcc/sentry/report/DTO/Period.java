package br.uva.tcc.sentry.report.DTO;

import java.time.LocalDate;

import br.uva.tcc.sentry.report.ReportFilter;

public record Period(LocalDate from, LocalDate to, int days) {

    public static Period of(ReportFilter filter) {
        return new Period(filter.from(), filter.to(), filter.days());
    }
}
