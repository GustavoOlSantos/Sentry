/** Extrai uma mensagem amigável de um erro do axios (o backend responde { status, error, message }). */
export function getApiErrorMessage(error, fallback = "Ocorreu um erro inesperado.") {
    if (error?.response?.data?.message) {
        return error.response.data.message;
    }

    if (error?.response) {
        return fallback;
    }

    if (error?.request) {
        return "Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.";
    }

    return fallback;
}
