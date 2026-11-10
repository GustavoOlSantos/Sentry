package br.uva.tcc.sentry.auth.service;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;

import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.repository.UserRepository;

/** Dublês compartilhados pelos testes de autenticação: repositório em memória e encoder falso. */
final class AuthTestSupport {

    private AuthTestSupport() {
    }

    /** Não faz hash de verdade: só precisa ser determinístico e contar as comparações. */
    static class FakePasswordEncoder implements PasswordEncoder {
        int matchesCalls = 0;

        @Override
        public String encode(CharSequence raw) {
            return "enc(" + raw + ")";
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            matchesCalls++;
            return encoded.equals("enc(" + raw + ")");
        }
    }

    /** Repositório em memória: implementa só o que os services usam. */
    static UserRepository inMemoryRepository(List<User> store) {
        long[] sequence = { 0 };

        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[] { UserRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmail" -> store.stream().filter(u -> u.getEmail().equals(args[0])).findFirst();
                    case "existsByEmail" -> store.stream().anyMatch(u -> u.getEmail().equals(args[0]));
                    case "existsByEmailAndIdNot" -> store.stream()
                            .anyMatch(u -> u.getEmail().equals(args[0]) && !u.getId().equals(args[1]));
                    case "countByRoleAndActiveTrue" -> store.stream()
                            .filter(u -> u.getRole() == args[0] && u.isActive()).count();
                    case "findAll" -> new ArrayList<>(store);
                    case "findById" -> store.stream().filter(u -> u.getId().equals(args[0])).findFirst();
                    case "count" -> (long) store.size();
                    case "save", "saveAndFlush" -> {
                        User user = (User) args[0];
                        if (user.getId() == null) {
                            user.setId(++sequence[0]);
                            store.add(user);
                        }
                        yield user;
                    }
                    case "delete" -> store.remove(args[0]);
                    case "toString" -> "InMemoryUserRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    static User user(Long id, String email, String hash, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    static Optional<User> find(List<User> store, String email) {
        return store.stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }
}
