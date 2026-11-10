/**
 * Perfis e permissões do Sentry. Espelha a matriz do backend (SecurityConfig):
 * o backend é quem de fato barra a ação; aqui os perfis servem para esconder o
 * que o usuário não poderia fazer e para proteger as rotas da interface.
 */

export const ROLES = {
    ADM: "ADM",
    DEV: "DEV",
    USER: "USER",
};

export const ALL_ROLES = [ROLES.ADM, ROLES.DEV, ROLES.USER];

export const ROLE_LABELS = {
    ADM: "Administrador",
    DEV: "Desenvolvedor",
    USER: "Usuário",
};

export const ROLE_DESCRIPTIONS = {
    ADM: "Acesso total: gerencia usuários, políticas e ativos.",
    DEV: "Trata findings, executa varreduras e edita ativos.",
    USER: "Somente leitura: acompanha ativos, findings e varreduras.",
};

/** Quais ações cada perfil pode realizar. */
export const PERMISSIONS = {
    manageUsers: [ROLES.ADM],
    writePolicies: [ROLES.ADM],
    deleteAssets: [ROLES.ADM],
    runScan: [ROLES.ADM, ROLES.DEV],
    editFindings: [ROLES.ADM, ROLES.DEV],
    editAssets: [ROLES.ADM, ROLES.DEV],
};

/**
 * Perfis que podem abrir cada rota. Use com o helper `guard` de
 * app/routes/protected.routes.jsx ao mover uma rota para as protegidas.
 */
export const ROUTE_ROLES = {
    "/": ALL_ROLES,
    "/board": ALL_ROLES,
    "/hosts": ALL_ROLES,
    "/scans": ALL_ROLES,
    "/reports": ALL_ROLES,
    "/policies": ALL_ROLES,
    "/perfil": ALL_ROLES,
    "/novo-scan": PERMISSIONS.runScan,
    "/usuarios": PERMISSIONS.manageUsers,
};

export function hasAnyRole(user, roles) {
    return !!user && roles.includes(user.role);
}

export function can(user, permission) {
    return hasAnyRole(user, PERMISSIONS[permission] ?? []);
}
