import api from "./api";

const policyService = {
    list: async () => {
        const response = await api.get("/policy");
        return response.data;
    },

    get: async (id) => {
        const response = await api.get(`/policy/${id}`);
        return response.data;
    },

    create: async (policy) => {
        const response = await api.post("/policy", policy);
        return response.data;
    },

    update: async (id, policy) => {
        const response = await api.put(`/policy/${id}`, policy);
        return response.data;
    },

    remove: async (id) => {
        await api.delete(`/policy/${id}`);
    },

    listRules: async () => {
        const response = await api.get("/policyRule");
        return response.data;
    },

    createRule: async (rule) => {
        const response = await api.post("/policyRule", rule);
        return response.data;
    },

    updateRule: async (id, rule) => {
        const response = await api.put(`/policyRule/${id}`, rule);
        return response.data;
    },

    removeRule: async (id) => {
        await api.delete(`/policyRule/${id}`);
    },
};

export default policyService;
