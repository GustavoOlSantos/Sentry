package br.uva.tcc.sentry.policy.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.uva.tcc.sentry.policy.domain.PolicyEvaluation;

public interface PolicyEvaluationRepository extends JpaRepository<PolicyEvaluation, Long> {

    @Modifying
    @Query("DELETE FROM PolicyEvaluation pe WHERE pe.scan.id = :scanId")
    int deleteByScanId(@Param("scanId") UUID scanId);
}
