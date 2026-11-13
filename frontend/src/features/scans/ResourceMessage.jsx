import { Link } from "react-router-dom";

import { getApiErrorMessage } from "../../services/apiError";

/** Mensagem de carregamento / não encontrado / erro das telas de varredura. Não renderiza nada se há dados. */
export default function ResourceMessage({ loading, error, data, notFoundText, backTo, backLabel }) {
    if (data) return null;

    if (loading) {
        return <p className="users-message">Carregando...</p>;
    }

    if (error?.response?.status === 404) {
        return (
            <div className="scans-empty">
                <i className="fa-solid fa-magnifying-glass" />
                <p>{notFoundText}</p>
                <Link className="users-btn users-btn--ghost" to={backTo}>{backLabel}</Link>
            </div>
        );
    }

    if (error) {
        return (
            <p className="auth-alert auth-alert--error" role="alert">
                <i className="fa-solid fa-circle-exclamation" />
                {getApiErrorMessage(error, "Não foi possível carregar os dados.")}
            </p>
        );
    }

    return null;
}
