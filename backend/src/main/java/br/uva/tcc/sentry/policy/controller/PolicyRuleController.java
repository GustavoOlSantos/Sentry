package br.uva.tcc.sentry.policy.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.policy.service.PolicyRuleService;

@RestController
@RequestMapping("/policyRule")
public class PolicyRuleController {

    private final PolicyRuleService policyRuleService;

    public PolicyRuleController(PolicyRuleService policyRuleService) {
        this.policyRuleService = policyRuleService;
    }

    @PostMapping
    public ResponseEntity<PolicyRule> createPolicyRule(@RequestBody PolicyRule policyRule) {
        PolicyRule createdRule = policyRuleService.createPolicyRule(policyRule);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRule);
    }

    @GetMapping
    public ResponseEntity<List<PolicyRule>> findAllPolicyRules() {
        List<PolicyRule> rules = policyRuleService.findAllPolicyRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyRule> findById(@PathVariable Long id) {
        Optional<PolicyRule> policyRule = policyRuleService.findById(id);
        return policyRule.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyRule> editPolicyRule(@PathVariable Long id, @RequestBody PolicyRule policyRule) {
        Optional<PolicyRule> existingRule = policyRuleService.findById(id);
        if (existingRule.isPresent()) {
            policyRule.setId(id); // Garante que atualiza o ID correto (ajuste caso o método de setter seja diferente)
            PolicyRule updatedRule = policyRuleService.editPolicyRule(policyRule);
            return ResponseEntity.ok(updatedRule);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicyRule(@PathVariable Long id) {
        Optional<PolicyRule> policyRule = policyRuleService.findById(id);
        if (policyRule.isPresent()) {
            policyRuleService.deletePolicyRule(policyRule.get());
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}