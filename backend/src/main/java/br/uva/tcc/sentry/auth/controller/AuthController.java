package br.uva.tcc.sentry.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uva.tcc.sentry.auth.DTO.ChangePasswordRequest;
import br.uva.tcc.sentry.auth.DTO.LoginRequest;
import br.uva.tcc.sentry.auth.DTO.LoginResponse;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Login (público) e operações do próprio usuário autenticado. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(currentUserId(authentication));
    }

    /** Troca a senha de quem está logado; depois disso os tokens antigos deixam de valer. */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    /** O nome da autenticação é o id do usuário (subject do token). */
    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
