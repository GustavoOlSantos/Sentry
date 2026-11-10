package br.uva.tcc.sentry.policy.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.policy.domain.Policy;
import br.uva.tcc.sentry.policy.repository.PolicyRepository;

@Service
public class PolicyService {

    private PolicyRepository policyRepository;
    
    public PolicyService(PolicyRepository policyRepository){
        this.policyRepository = policyRepository;
    }

    public Policy createPolicy(Policy policy){
        return policyRepository.save(policy);
    }

    public List<Policy> findAllPolicies(){
        return policyRepository.findAll();
    }

    public Optional<Policy> findById(Long id){
        return policyRepository.findById(id);
    }

    public void deletePolicy(Policy policy){
        policyRepository.delete(policy);
    }

    public Policy editPolicy(Policy policy){
        return policyRepository.save(policy);
    }
}
