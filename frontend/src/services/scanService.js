import api from './api';

/** Consultas das telas de varreduras (grupos de scan e scans individuais). Só leitura. */
const scanService = {
    /** Grupos mais recentes, com status derivado, scans por status, ativos e findings. */
    async listGroups() {
        const { data } = await api.get('/scanGroup');
        return data;
    },

    /** { group, scans[] } */
    async getGroup(groupId) {
        const { data } = await api.get(`/scanGroup/id/${groupId}`);
        return data;
    },

    /** Comando, avisos, erro, ativos encontrados e findings detectados por um scan. */
    async getScan(scanId) {
        const { data } = await api.get(`/scan/id/${scanId}`);
        return data;
    },
};

export default scanService;
