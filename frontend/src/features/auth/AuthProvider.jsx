import { useCallback, useEffect, useMemo, useState } from "react";

import authService from "../../services/authService";
import { SESSION_EXPIRED_EVENT, getToken, getTokenExpiration } from "../../services/session";

import { AuthContext } from "./AuthContext";
import { can as canDo, hasAnyRole } from "./roles";

// setTimeout não aceita atrasos acima de ~24,8 dias
const MAX_TIMEOUT_MS = 2 ** 31 - 1;

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => (authService.isAuthenticated() ? authService.getUser() : null));
    const [initializing, setInitializing] = useState(() => authService.isAuthenticated());
    const [sessionExpired, setSessionExpired] = useState(false);
    // aviso a ser exibido na tela de login (ex.: "senha alterada"). Fica no contexto, e não no state da
    // navegação, porque a ProtectedRoute redireciona para /login por conta própria ao perder o usuário.
    const [notice, setNotice] = useState(null);

    // 1) Ao abrir o app, confirma a sessão guardada com o servidor: perfil e status podem ter mudado.
    useEffect(() => {
        if (!authService.isAuthenticated()) {
            authService.logout(); // limpa token vencido ou lixo do localStorage
            return;
        }

        let cancelled = false;

        authService.me()
            .then((current) => {
                if (!cancelled) setUser(current);
            })
            .catch((err) => {
                // 401: o interceptor já limpou a sessão e avisou; outros erros (servidor fora) mantêm o usuário guardado
                if (!cancelled && err.response?.status === 401) setUser(null);
            })
            .finally(() => {
                if (!cancelled) setInitializing(false);
            });

        return () => {
            cancelled = true;
        };
    }, []);

    // 2) Qualquer 401 da API (token inválido/expirado, usuário desativado) encerra a sessão.
    useEffect(() => {
        const onExpired = () => {
            setUser(null);
            setSessionExpired(true);
        };

        window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
        return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
    }, []);

    // 3) Desloga sozinho no instante em que o token expira.
    useEffect(() => {
        if (!user) return;

        const expiration = getTokenExpiration(getToken());
        if (!expiration) return;

        const delay = Math.min(Math.max(expiration - Date.now(), 0), MAX_TIMEOUT_MS);

        const timer = setTimeout(() => {
            authService.logout();
            setUser(null);
            setSessionExpired(true);
        }, delay);

        return () => clearTimeout(timer);
    }, [user]);

    const login = useCallback(async (email, password) => {
        const loggedUser = await authService.login(email, password);

        setUser(loggedUser);
        setSessionExpired(false);
        setNotice(null);
        return loggedUser;
    }, []);

    /** Encerra a sessão. `message` (opcional) é exibida na tela de login. */
    const logout = useCallback((message = null) => {
        authService.logout();
        setUser(null);
        setSessionExpired(false);
        setNotice(typeof message === "string" ? message : null);
    }, []);

    /** Recarrega o usuário logado (após editar o próprio cadastro, por exemplo). */
    const refreshUser = useCallback(async () => {
        const current = await authService.me();

        setUser(current);
        return current;
    }, []);

    const value = useMemo(() => ({
        user,
        initializing,
        sessionExpired,
        notice,
        isAuthenticated: !!user,
        login,
        logout,
        refreshUser,
        hasRole: (...roles) => hasAnyRole(user, roles),
        can: (permission) => canDo(user, permission),
    }), [user, initializing, sessionExpired, notice, login, logout, refreshUser]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthProvider;
