import api from './api';
import {
    clearSession,
    getStoredUser,
    getToken,
    isTokenExpired,
    saveSession,
    saveUser,
} from './session';

const authService = {
    /** Faz o login, guarda o token e devolve o usuário. */
    async login(email, password) {
        const { data } = await api.post('/auth/login', { email, password });

        saveSession(data.accessToken, data.user);
        return data.user;
    },

    /** Dados atuais do usuário logado (a role vem do banco, não do token). */
    async me() {
        const { data } = await api.get('/auth/me');

        saveUser(data);
        return data;
    },

    /** Troca a própria senha. O servidor invalida os tokens antigos: faça logout em seguida. */
    async changePassword(currentPassword, newPassword) {
        await api.put('/auth/me/password', { currentPassword, newPassword });
    },

    logout() {
        clearSession();
    },

    isAuthenticated() {
        const token = getToken();
        return !!token && !isTokenExpired(token);
    },

    getUser: getStoredUser,
    getToken,
};

export default authService;
