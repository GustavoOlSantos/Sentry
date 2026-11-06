package br.uva.tcc.sentry.policy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.policy.domain.PolicyStatus;

/**
 * {@link JpaRepository} já cobre todo o CRUD (Insert/Update via save,
 * Delete via deleteById/delete, Select via findById/findAll) sem precisar
 * escrever nada manualmente. Os métodos abaixo são consultas extras
 * usadas pelo avaliador de políticas ({@code PolicyRuleEvaluator}).
 */
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    List<PolicyRule> findByPolicyId(Long policyId);

    /**
     * Regras habilitadas pertencentes a uma política com o status
     * informado (normalmente {@link PolicyStatus#ACTIVE}). Usa
     * {@code JOIN FETCH} pra trazer a {@code Policy} junto na mesma
     * consulta — sem isso, {@code rule.getPolicy().getName()} lançaria
     * {@code LazyInitializationException} quando acessado fora da
     * transação do repository (como acontece no avaliador, chamado a
     * partir do consumer do scan).
     */
    @Query("SELECT r FROM PolicyRule r JOIN FETCH r.policy p WHERE r.enabled = true AND p.status = :status")
    List<PolicyRule> findActiveEnabledRules(@Param("status") PolicyStatus status);
}
