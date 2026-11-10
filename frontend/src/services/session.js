/**
 * Armazenamento da sessão no navegador (token JWT + snapshot do usuário).
 *
 * Fica em um módulo próprio, sem importar o api.js, para o api.js poder usá-lo
 * nos interceptors sem criar dependência circular com o authService.
 *
 * Observação: o token em localStorage fica exposto a XSS. É o trade-off comum
 * de SPAs com Bearer token; por isso o frontend não deve renderizar HTML vindo
 * da API (o React já escapa o conteúdo por padrão).
 */

const TOKEN_KEY = "token"; // mesma chave que a ProtectedRoute original já usava
const USER_KEY = "user";

/** Disparado pelo interceptor quando a API responde 401 (token inválido, expirado ou usuário desativado). */
export const SESSION_EXPIRED_EVENT = "sentry:session-expired";

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser() {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY));
    } catch {
        return null;
    }
}

export function saveSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function saveUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export function notifySessionExpired() {
    window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));
}

/** Lê o payload do JWT (sem validar a assinatura — isso é papel do servidor). */
export function decodeToken(token) {
    try {
        const payload = token.split(".")[1];
        const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
        const json = decodeURIComponent(
            atob(base64)
                .split("")
                .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
                .join("")
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
}

/** Momento de expiração do token, em milissegundos desde a época; null se ilegível. */
export function getTokenExpiration(token) {
    const exp = token ? decodeToken(token)?.exp : null;
    return typeof exp === "number" ? exp * 1000 : null;
}

export function isTokenExpired(token) {
    const expiration = getTokenExpiration(token);
    return expiration === null || expiration <= Date.now();
}
