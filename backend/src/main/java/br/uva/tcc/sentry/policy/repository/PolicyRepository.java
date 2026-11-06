package br.uva.tcc.sentry.policy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uva.tcc.sentry.policy.domain.Policy;
import br.uva.tcc.sentry.policy.domain.PolicyStatus;

/**
 * {@link JpaRepository} já cobre todo o CRUD (Insert/Update via save,
 * Delete via deleteById/delete, Select via findById/findAll) sem precisar
 * escrever nada manualmente. Os métodos abaixo são só consultas extras
 * úteis pro resto da aplicação.
 */
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByStatus(PolicyStatus status);
}
