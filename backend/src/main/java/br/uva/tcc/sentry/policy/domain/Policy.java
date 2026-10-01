package br.uva.tcc.sentry.policy.domain;

import java.util.UUID;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 * Representa um conjunto de regras políticas de segurança internas
 */
@Getter 
@Setter 
public class Policy {

    private UUID id;
    private String name;
    private String description;
    private PolicyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
