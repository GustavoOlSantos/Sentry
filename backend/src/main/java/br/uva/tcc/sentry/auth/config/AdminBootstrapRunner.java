package br.uva.tcc.sentry.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.auth.DTO.CreateUserRequest;
import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.repository.UserRepository;
import br.uva.tcc.sentry.auth.service.UserService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final SecurityProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        SecurityProperties.BootstrapAdmin admin = properties.bootstrapAdmin();

        if (!admin.isConfigured()) {
            log.warn("Nenhum usuário cadastrado e SENTRY_ADMIN_EMAIL/SENTRY_ADMIN_PASSWORD não foram definidos: "
                    + "ninguém conseguirá fazer login.");
            return;
        }

        userService.create(new CreateUserRequest(admin.name(), admin.email(), admin.password(), Role.ADM));
        log.info("Administrador inicial criado: {}", admin.email());
    }
}
