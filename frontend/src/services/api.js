import axios from 'axios';

import { clearSession, getToken, notifySessionExpired } from './session';

const API_URL = import.meta.env.VITE_SENTRY_API;

if(API_URL == undefined || API_URL.trim() == ""){
  console.warn("Variáveis de ambiente não configuradas.");
}

const api = axios.create({
    baseURL: API_URL
});

// Anexa o token JWT em toda requisição
api.interceptors.request.use(config => {
    const token = getToken();

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

// 401 = sessão inválida (token expirado/adulterado ou usuário desativado): encerra a sessão local.
// 403 = autenticado, mas o perfil não permite a ação: NÃO desloga, só deixa o erro seguir
// para a tela tratar (diferente do 401, o token continua válido).
api.interceptors.response.use(
    (response) => response,

    (error) => {
        const status = error.response?.status;
        const url = error.config?.url || "";

        const isLoginRoute = url.includes("/auth/login");

        if (status === 401 && !isLoginRoute && getToken()) {
            clearSession();
            notifySessionExpired();
        }

        return Promise.reject(error);
    }
);

export default api;
