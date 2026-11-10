import { useContext } from "react";

import { AuthContext } from "./AuthContext";

/**
 * Acesso à sessão atual:
 * { user, initializing, isAuthenticated, sessionExpired, notice, login, logout(mensagem?), refreshUser, hasRole(...roles), can(permissao) }
 */
export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth deve ser usado dentro de <AuthProvider>.");
    }

    return context;
}
