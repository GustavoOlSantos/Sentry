import api from './api';

/** CRUD de usuários (as rotas /users exigem perfil ADM no backend). */
const userService = {
    async list() {
        const { data } = await api.get('/users');
        return data;
    },

    async get(id) {
        const { data } = await api.get(`/users/${id}`);
        return data;
    },

    async create({ name, email, password, role }) {
        const { data } = await api.post('/users', { name, email, password, role });
        return data;
    },

    async update(id, { name, email, role, active }) {
        const { data } = await api.put(`/users/${id}`, { name, email, role, active });
        return data;
    },

    async resetPassword(id, newPassword) {
        await api.put(`/users/${id}/password`, { newPassword });
    },

    async remove(id) {
        await api.delete(`/users/${id}`);
    },
};

export default userService;
