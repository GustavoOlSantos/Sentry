package br.uva.tcc.sentry.docs.api;

import org.springframework.security.core.Authentication;

import br.uva.tcc.sentry.auth.DTO.ChangePasswordRequest;
import br.uva.tcc.sentry.auth.DTO.LoginRequest;
import br.uva.tcc.sentry.auth.DTO.LoginResponse;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.AuthExamples;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** Documentação de {@code /auth}. Implementada por {@code AuthController}. */
@Tag(name = ApiTags.AUTH)
public interface AuthApi {

    @Operation(
        operationId = "login",
        summary = "Autenticar (login)",
        description = """
            Valida e-mail e senha e devolve um **token JWT** (HS256) para usar no header `Authorization: Bearer <token>`.

            - **Acesso:** público (não exige token).
            - O e-mail é normalizado (sem espaços nas pontas e em minúsculas) antes da busca.
            - Por segurança, a resposta é a **mesma** para e-mail inexistente, senha errada e usuário desativado.
            - `expiresIn` é a validade do token, em segundos, a partir da emissão.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class),
                examples = @ExampleObject(name = "Login bem-sucedido", value = AuthExamples.LOGIN_RESPONSE))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido: e-mail ou senha ausentes/em branco, ou JSON ausente/malformado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Campos obrigatórios", value = ErrorExamples.VALIDATION_LOGIN),
                    @ExampleObject(name = "Corpo ausente ou malformado", value = ErrorExamples.UNREADABLE_BODY)
                })),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas (e-mail inexistente, senha incorreta ou usuário desativado).",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Credenciais inválidas", value = ErrorExamples.INVALID_CREDENTIALS)))
    })
    LoginResponse login(
        @RequestBody(description = "Credenciais do usuário.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginRequest.class),
                examples = @ExampleObject(name = "Login", value = AuthExamples.LOGIN_REQUEST)))
        LoginRequest request);

    @Operation(
        operationId = "getCurrentUser",
        summary = "Dados do usuário autenticado",
        description = """
            Devolve o usuário dono do token enviado (sem o hash da senha).

            - **Acesso:** qualquer usuário autenticado (`ADM`, `DEV` ou `USER`).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário autenticado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(name = "Usuário", value = AuthExamples.USER))),
        @ApiResponse(responseCode = "404", description = "Usuário do token não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Usuário não encontrado", value = ErrorExamples.USER_NOT_FOUND)))
    })
    @ApiUnauthorized
    UserResponse me(@Parameter(hidden = true) Authentication authentication);

    @Operation(
        operationId = "changeOwnPassword",
        summary = "Trocar a própria senha",
        description = """
            Troca a senha de quem está logado. **Depois da troca, todos os tokens emitidos antes deixam de valer** — \
            o usuário precisa fazer login novamente.

            - **Acesso:** qualquer usuário autenticado.
            - Regras da nova senha: mínimo de 8 caracteres, máximo de 72 **bytes** (UTF-8) e diferente da atual.
            - Senha atual incorreta retorna **400** (e não 401) para que o frontend não deslogue o usuário por engano.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso (sem corpo)."),
        @ApiResponse(responseCode = "400",
            description = "Corpo inválido, senha atual incorreta ou nova senha fora da política.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Campos obrigatórios", value = ErrorExamples.VALIDATION_PASSWORD),
                    @ExampleObject(name = "Senha atual incorreta", value = ErrorExamples.INVALID_PASSWORD_CURRENT),
                    @ExampleObject(name = "Nova senha curta", value = ErrorExamples.INVALID_PASSWORD_SHORT),
                    @ExampleObject(name = "Nova senha longa demais", value = ErrorExamples.INVALID_PASSWORD_LONG),
                    @ExampleObject(name = "Nova senha igual à atual", value = ErrorExamples.INVALID_PASSWORD_SAME),
                    @ExampleObject(name = "Corpo ausente ou malformado", value = ErrorExamples.UNREADABLE_BODY)
                })),
        @ApiResponse(responseCode = "404", description = "Usuário do token não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Usuário não encontrado", value = ErrorExamples.USER_NOT_FOUND)))
    })
    @ApiUnauthorized
    ResponseEntity<Void> changePassword(
        @Parameter(hidden = true) Authentication authentication,
        @RequestBody(description = "Senha atual e nova senha.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ChangePasswordRequest.class),
                examples = @ExampleObject(name = "Troca de senha", value = AuthExamples.CHANGE_PASSWORD_REQUEST)))
        ChangePasswordRequest request);
}
