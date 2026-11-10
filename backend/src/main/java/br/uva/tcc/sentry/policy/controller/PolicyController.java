package br.uva.tcc.sentry.policy.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.uva.tcc.sentry.policy.domain.Policy;
import br.uva.tcc.sentry.policy.service.PolicyService;

@RestController
@RequestMapping("/policy")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<Policy> createPolicy(@RequestBody Policy policy) {
        Policy createdPolicy = policyService.createPolicy(policy);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPolicy);
    }

    @GetMapping
    public ResponseEntity<List<Policy>> findAllPolicies() {
        List<Policy> policies = policyService.findAllPolicies();
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Policy> findById(@PathVariable Long id) {
        Optional<Policy> policy = policyService.findById(id);
        return policy.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Policy> editPolicy(@PathVariable Long id, @RequestBody Policy policy) {
        Optional<Policy> existingPolicy = policyService.findById(id);
        if (existingPolicy.isPresent()) {
            policy.setId(id);
            Policy updatedPolicy = policyService.editPolicy(policy);
            return ResponseEntity.ok(updatedPolicy);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        Optional<Policy> policy = policyService.findById(id);
        if (policy.isPresent()) {
            policyService.deletePolicy(policy.get());
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}