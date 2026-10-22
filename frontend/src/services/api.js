import axios from 'axios';

const API_URL = import.meta.env.VITE_SENTRY_API;

if(API_URL == undefined || API_URL.trim() == ""){
  console.warn("Variáveis de ambiente não configuradas.");
}

const api = axios.create({
    baseURL: API_URL
});

api.interceptors.request.use(config => {

    /*const token = localStorage.getItem("token");

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }*/

    return config;
});

/*api.interceptors.response.use(
  (response) => response,

  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || "";

    const isAuthRoute =
      url.includes("/auth/login") ||
      url.includes("/auth/cadastro");

    if (
      (status === 401 || status === 403) &&
      !isAuthRoute &&
      authService.isAuthenticated
    ) {
      authService.autoLogout();
    }

    return Promise.reject(error);
  }
);*/


export default api;
