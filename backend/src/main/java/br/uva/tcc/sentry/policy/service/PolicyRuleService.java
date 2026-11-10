package br.uva.tcc.sentry.policy.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.policy.repository.PolicyRuleRepository;

@Service
public class PolicyRuleService {

    private PolicyRuleRepository policyRuleRepository;
    
    public PolicyRuleService(PolicyRuleRepository policyRuleRepository){
        this.policyRuleRepository = policyRuleRepository;
    }

    public PolicyRule createPolicyRule(PolicyRule policyRule){
        return policyRuleRepository.save(policyRule);
    }

    public List<PolicyRule> findAllPolicyRules(){
        return policyRuleRepository.findAll();
    }

    public Optional<PolicyRule> findById(Long id){
        return policyRuleRepository.findById(id);
    }

    public void deletePolicyRule(PolicyRule policyRule){
        policyRuleRepository.delete(policyRule);
    }

    public PolicyRule editPolicyRule(PolicyRule policyRule){
        return policyRuleRepository.save(policyRule);
    }
}
