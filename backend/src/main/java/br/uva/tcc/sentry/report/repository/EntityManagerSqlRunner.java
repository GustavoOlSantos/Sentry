package br.uva.tcc.sentry.report.repository;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Component
public class EntityManagerSqlRunner implements SqlRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> query(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach((k, v) -> {
            if (sql.contains(":" + k)) {
                query.setParameter(k, v);
            }
        });

        List<?> rows = query.getResultList();

        return rows.stream()
                .map(row -> row instanceof Object[] array ? array : new Object[] { row })
                .toList();
    }
}
