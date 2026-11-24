package br.uva.tcc.sentry.docs.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados globais da documentação OpenAPI (título, descrição, grupos e autenticação).
 * A documentação de cada endpoint está nas interfaces de {@code br.uva.tcc.sentry.docs.api}.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Sentry API",
        version = "0.0.1",
        description = """
            API do **Sentry**, sistema de gestão de vulnerabilidades: executa scans (Nmap), consulta CVEs no NVD, \
            avalia políticas de configuração e consolida tudo em *findings* acompanhados em um board.

            ## Autenticação
            1. Faça `POST /auth/login` com e-mail e senha.
            2. Envie o `accessToken` retornado no header `Authorization: Bearer <token>` (no Swagger UI, use o botão **Authorize**).

            O token expira (ver `expiresIn`, em segundos) e deixa de valer se o usuário for desativado, excluído ou trocar a senha.

            ## Perfis de acesso (roles)
            | Perfil | O que pode fazer |
            |--------|------------------|
            | `ADM`  | Tudo: usuários, exclusão de assets/scans, políticas e regras. |
            | `DEV`  | Leitura, tratar findings (status e responsável), executar scans e editar assets. |
            | `USER` | Somente leitura. |

            ## Formato padrão de erro
            Erros tratados pela aplicação (e pelos filtros de segurança) devolvem JSON com `timestamp`, `status`, `error` e `message`. \
            Exceções: algumas validações feitas direto em controllers devolvem **texto puro** ou `{ "message": "..." }` e \
            alguns `404` de políticas/regras vêm **sem corpo** — isso está indicado em cada endpoint.
            """
    ),
    tags = {
        @Tag(name = ApiTags.AUTH, description = "Login e operações do próprio usuário autenticado."),
        @Tag(name = ApiTags.USERS, description = "Gestão de usuários do sistema."),
        @Tag(name = ApiTags.ASSETS, description = "Ativos (hosts) descobertos pelos scans, com seus serviços e findings."),
        @Tag(name = ApiTags.FINDINGS, description = "Board de findings (vulnerabilidades e violações de política): status e responsável."),
        @Tag(name = ApiTags.SCANS, description = "Disparo e consulta de scans individuais (um alvo por scan)."),
        @Tag(name = ApiTags.SCAN_GROUPS, description = "Grupos de scan: cada chamada de `POST /scan/exec` cria um grupo com um scan por alvo."),
        @Tag(name = ApiTags.POLICIES, description = "Políticas de segurança avaliadas nos ativos."),
        @Tag(name = ApiTags.POLICY_RULES, description = "Regras que compõem as políticas."),
        @Tag(name = ApiTags.REPORTS, description = "Indicadores e relatório consolidado (JSON e PDF). Todos aceitam filtros por período, ativo, scan, severidade etc.")
    }
)
@SecurityScheme(
    name = ApiTags.BEARER_AUTH,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtido em `POST /auth/login`. Informe apenas o token (o prefixo `Bearer` é adicionado automaticamente)."
)
public class OpenApiConfig {
}
