package br.uva.tcc.sentry.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Usuário do Sentry. O login é pelo e-mail (sempre gravado em minúsculas).
 *
 * Esta entidade NUNCA deve ser devolvida pela API: use {@code UserResponse},
 * que não carrega o hash da senha. O {@code toString} também o omite, para que
 * não vá parar em log.
 */
@Entity
@Table(name = "app_user",
       uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"))
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    /** Hash BCrypt (60 caracteres); a senha em si nunca é guardada. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    /** Usuário desativado não faz login e tem os tokens já emitidos recusados. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Tokens emitidos antes deste momento deixam de valer (troca/reset de senha). */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.passwordChangedAt == null) {
            this.passwordChangedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "User {id=" + id + ", email=" + email + ", role=" + role + ", active=" + active + "}";
    }
}
