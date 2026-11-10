package br.uva.tcc.sentry.auth.config;

import java.lang.reflect.Proxy;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;

import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.repository.UserRepository;

final class BootstrapTestSupport {

    private BootstrapTestSupport() {
    }

    static PasswordEncoder encoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                return "enc(" + raw + ")";
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                return encoded.equals("enc(" + raw + ")");
            }
        };
    }

    static UserRepository repository(List<User> store) {
        long[] sequence = { 0 };
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[] { UserRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "count" -> (long) store.size();
                    case "existsByEmail" -> store.stream().anyMatch(u -> u.getEmail().equals(args[0]));
                    case "saveAndFlush", "save" -> {
                        User user = (User) args[0];
                        if (user.getId() == null) {
                            user.setId(++sequence[0]);
                            store.add(user);
                        }
                        yield user;
                    }
                    case "toString" -> "InMemoryUserRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
