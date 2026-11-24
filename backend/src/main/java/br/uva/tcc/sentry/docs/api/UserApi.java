package br.uva.tcc.sentry.docs.api;

import java.util.List;

import org.springframework.http.ResponseEntity;

import br.uva.tcc.sentry.auth.DTO.CreateUserRequest;
import br.uva.tcc.sentry.auth.DTO.ResetPasswordRequest;
import br.uva.tcc.sentry.auth.DTO.UpdateUserRequest;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.docs.annotation.ApiForbidden;
import br.uva.tcc.sentry.docs.annotation.ApiInvalidParameter;
import br.uva.tcc.sentry.docs.annotation.ApiUnauthorized;
import br.uva.tcc.sentry.docs.config.ApiTags;
import br.uva.tcc.sentry.docs.example.AuthExamples;
import br.uva.tcc.sentry.docs.example.ErrorExamples;
import br.uva.tcc.sentry.docs.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Documentação de {@code /users}. Implementada por {@code UserController}. */
@Tag(name = ApiTags.USERS)
public interface UserApi {

    @Operation(
        operationId = "listUsers",
        summary = "Listar usuários",
        description = """
            Lista todos os usuários (sem o hash da senha).

            - **Acesso:** qualquer usuário autenticado (`ADM`, `DEV` ou `USER`). \
            As demais rotas de `/users` exigem `ADM`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Lista de usuários.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)),
                examples = @ExampleObject(name = "Usuários", value = AuthExamples.USER_LIST))))
    @ApiUnauthorized
    List<UserResponse> findAll();

    @Operation(
        operationId = "getUserById",
        summary = "Buscar usuário por id",
        description = "- **Acesso:** somente `ADM`.",
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(name = "Usuário", value = AuthExamples.USER))),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Usuário não encontrado", value = ErrorExamples.USER_NOT_FOUND)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    UserResponse findById(
        @Parameter(in = ParameterIn.PATH, description = "Identificador do usuário.", example = "2") Long id);

    @Operation(
        operationId = "createUser",
        summary = "Criar usuário",
        description = """
            Cria um usuário **ativo** com a senha informada.

            - **Acesso:** somente `ADM`.
            - O e-mail é normalizado (sem espaços nas pontas e em minúsculas) e deve ser único.
            - Senha: mínimo de 8 caracteres e máximo de 72 **bytes** (UTF-8).
            - `role`: `ADM`, `DEV` ou `USER`.
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(name = "Usuário criado", value = AuthExamples.USER))),
        @ApiResponse(responseCode = "400",
            description = "Dados inválidos (campos obrigatórios, e-mail inválido, tamanhos máximos), senha fora da política, "
                    + "`role` inexistente ou corpo ausente/malformado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Campos inválidos", value = ErrorExamples.VALIDATION_CREATE_USER),
                    @ExampleObject(name = "Senha curta", value = ErrorExamples.INVALID_PASSWORD_SHORT),
                    @ExampleObject(name = "Senha longa demais", value = ErrorExamples.INVALID_PASSWORD_LONG),
                    @ExampleObject(name = "Corpo malformado ou role inexistente", value = ErrorExamples.UNREADABLE_BODY)
                })),
        @ApiResponse(responseCode = "409", description = "Já existe um usuário com este e-mail.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "E-mail em uso", value = ErrorExamples.EMAIL_IN_USE)))
    })
    @ApiUnauthorized
    @ApiForbidden
    UserResponse create(
        @RequestBody(description = "Dados do novo usuário.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CreateUserRequest.class),
                examples = @ExampleObject(name = "Novo usuário", value = AuthExamples.CREATE_USER_REQUEST)))
        CreateUserRequest request);

    @Operation(
        operationId = "updateUser",
        summary = "Atualizar usuário",
        description = """
            Substitui os dados editáveis do usuário (nome, e-mail, perfil e situação). A senha tem endpoint próprio.

            - **Acesso:** somente `ADM`.
            - Não é permitido rebaixar ou desativar o **único administrador ativo** do sistema (409).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(name = "Usuário atualizado", value = AuthExamples.USER))),
        @ApiResponse(responseCode = "400",
            description = "Dados inválidos, `id` com formato inválido, `role` inexistente ou corpo ausente/malformado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Campos inválidos", value = ErrorExamples.VALIDATION_UPDATE_USER),
                    @ExampleObject(name = "Corpo malformado ou role inexistente", value = ErrorExamples.UNREADABLE_BODY),
                    @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                })),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Usuário não encontrado", value = ErrorExamples.USER_NOT_FOUND))),
        @ApiResponse(responseCode = "409",
            description = "E-mail já usado por outro usuário, ou a alteração deixaria o sistema sem administrador ativo.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "E-mail em uso", value = ErrorExamples.EMAIL_IN_USE),
                    @ExampleObject(name = "Último administrador", value = ErrorExamples.LAST_ADMIN_DEMOTE)
                }))
    })
    @ApiUnauthorized
    @ApiForbidden
    UserResponse update(
        @Parameter(in = ParameterIn.PATH, description = "Identificador do usuário.", example = "2") Long id,
        @RequestBody(description = "Novos dados do usuário (substituição completa).", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UpdateUserRequest.class),
                examples = @ExampleObject(name = "Atualização", value = AuthExamples.UPDATE_USER_REQUEST)))
        UpdateUserRequest request);

    @Operation(
        operationId = "resetUserPassword",
        summary = "Redefinir a senha de um usuário",
        description = """
            Define uma nova senha para o usuário, sem exigir a senha atual. **Invalida os tokens já emitidos** para ele.

            - **Acesso:** somente `ADM`.
            - Senha: mínimo de 8 caracteres e máximo de 72 **bytes** (UTF-8).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha redefinida (sem corpo)."),
        @ApiResponse(responseCode = "400",
            description = "Senha ausente ou fora da política, `id` com formato inválido ou corpo ausente/malformado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Campo obrigatório", value = ErrorExamples.VALIDATION_PASSWORD),
                    @ExampleObject(name = "Senha curta", value = ErrorExamples.INVALID_PASSWORD_SHORT),
                    @ExampleObject(name = "Senha longa demais", value = ErrorExamples.INVALID_PASSWORD_LONG),
                    @ExampleObject(name = "Id inválido", value = ErrorExamples.INVALID_PARAMETER)
                })),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Usuário não encontrado", value = ErrorExamples.USER_NOT_FOUND)))
    })
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<Void> resetPassword(
        @Parameter(in = ParameterIn.PATH, description = "Identificador do usuário.", example = "2") Long id,
        @RequestBody(description = "Nova senha.", required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ResetPasswordRequest.class),
                examples = @ExampleObject(name = "Nova senha", value = AuthExamples.RESET_PASSWORD_REQUEST)))
        ResetPasswordRequest request);

    @Operation(
        operationId = "deleteUser",
        summary = "Excluir usuário",
        description = """
            Exclui o usuário.

            - **Acesso:** somente `ADM`.
            - Não é permitido excluir o **único administrador ativo** do sistema (409).
            """,
        security = @SecurityRequirement(name = ApiTags.BEARER_AUTH))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuário excluído (sem corpo)."),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Usuário não encontrado", value = ErrorExamples.USER_NOT_FOUND))),
        @ApiResponse(responseCode = "409", description = "A exclusão deixaria o sistema sem administrador ativo.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Último administrador", value = ErrorExamples.LAST_ADMIN_DELETE)))
    })
    @ApiInvalidParameter
    @ApiUnauthorized
    @ApiForbidden
    ResponseEntity<Void> delete(
        @Parameter(in = ParameterIn.PATH, description = "Identificador do usuário.", example = "2") Long id);
}
