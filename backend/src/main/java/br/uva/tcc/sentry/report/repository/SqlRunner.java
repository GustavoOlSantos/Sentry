package br.uva.tcc.sentry.report.repository;

import java.util.List;
import java.util.Map;

public interface SqlRunner {

    List<Object[]> query(String sql, Map<String, Object> params);
}
