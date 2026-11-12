import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { SideBar } from "../../components/sidebar";
import { useAuth } from "../auth/useAuth";
import policyService from "../../services/policyService";
import "../../styles/policies.css";

const STATUS_LABELS = {
    ACTIVE: "Ativa",
    INACTIVE: "Inativa",
};

function getApiErrorMessage(error, fallback) {
    if(error?.message == "Network Error") {
        error.message = "Não foi possível estabelecer conexão." 
    }
    return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
}

export default function Policies() {
    const navigate = useNavigate();
    const { can } = useAuth();

    const [policies, setPolicies] = useState([]);
    const [search, setSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("todos");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const loadPolicies = async () => {
        setLoading(true);
        try {
            const data = await policyService.list();
            setPolicies(Array.isArray(data) ? data : []);
            setError("");
        } catch (err) {
            console.error("Falha ao carregar políticas", err);
            setError(getApiErrorMessage(err, "Não foi possível carregar as políticas."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPolicies();
    }, []);

    const totals = useMemo(() => ({
        all: policies.length,
        active: policies.filter((policy) => policy.status === "ACTIVE").length,
        inactive: policies.filter((policy) => policy.status === "INACTIVE").length,
    }), [policies]);

    const filteredPolicies = useMemo(() => {
        const term = search.trim().toLowerCase();

        return policies.filter((policy) => {
            const matchesSearch =
                (policy.name || "").toLowerCase().includes(term) ||
                (policy.description || "").toLowerCase().includes(term);

            const matchesStatus =
                statusFilter === "todos" || policy.status === statusFilter;

            return matchesSearch && matchesStatus;
        });
    }, [policies, search, statusFilter]);

    return (
        <main className="policies-container">
            <SideBar />

            <section className="policies-content">
                <div className="policies-heading">
                    <div>
                        <h2>Políticas</h2>
                        <p>Defina critérios de segurança e regras que serão avaliados durante as varreduras.</p>
                    </div>

                    {can("writePolicies") && (
                        <button
                            type="button"
                            className="policies-btn policies-btn--primary"
                            onClick={() => navigate("/policies/nova")}
                        >
                            <i className="fa-solid fa-shield-halved" />
                            Nova política
                        </button>
                    )}
                </div>

                <section className="policies-overview">
                    <div className="policies-overview__card">
                        <i className="fa-solid fa-shield-halved" />
                        <div>
                            <h4>{totals.all}</h4>
                            <p>Políticas</p>
                        </div>
                    </div>

                    <div className="policies-overview__card">
                        <i className="fa-solid fa-circle-check" />
                        <div>
                            <h4>{totals.active}</h4>
                            <p>Ativas</p>
                        </div>
                    </div>

                    <div className="policies-overview__card">
                        <i className="fa-solid fa-circle-pause" />
                        <div>
                            <h4>{totals.inactive}</h4>
                            <p>Inativas</p>
                        </div>
                    </div>
                </section>

                {notice && (
                    <div className="policies-notice policies-notice--success" role="status">
                        <i className="fa-solid fa-circle-check" />
                        <span>{notice}</span>
                        <button type="button" onClick={() => setNotice("")} aria-label="Fechar aviso">&times;</button>
                    </div>
                )}

                <section className="policies-filters">
                    <div className="policies-filters__search">
                        <i className="fa-solid fa-magnifying-glass" />
                        <input
                            type="text"
                            placeholder="Buscar por nome ou descrição"
                            aria-label="Buscar políticas"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                        />
                    </div>

                    <div className="policies-filters__select">
                        <select
                            value={statusFilter}
                            onChange={(event) => setStatusFilter(event.target.value)}
                            aria-label="Filtrar políticas por status"
                        >
                            <option value="todos">Status: Todos</option>
                            <option value="ACTIVE">Ativas</option>
                            <option value="INACTIVE">Inativas</option>
                        </select>
                        <i className="fa-solid fa-chevron-down" aria-hidden="true" />
                    </div>

                    <button
                        type="button"
                        className="policies-btn policies-btn--ghost"
                        onClick={loadPolicies}
                        disabled={loading}
                    >
                        <i className={`fa-solid fa-rotate ${loading ? "fa-spin" : ""}`} />
                        {loading ? "Atualizando..." : "Atualizar"}
                    </button>
                </section>

                {error && (
                    <div className="policies-notice policies-notice--error" role="alert">
                        <i className="fa-solid fa-circle-exclamation" />
                        <span>{error}</span>
                    </div>
                )}

                <div className="policies-table-wrapper">
                    <table className="policies-table">
                        <thead>
                            <tr>
                                <th>Política</th>
                                <th>Status</th>
                                <th>Criada em</th>
                                <th>Atualizada em</th>
                                <th className="policies-table__actions-col">Ações</th>
                            </tr>
                        </thead>

                        <tbody>
                            {filteredPolicies.map((policy) => (
                                <tr
                                    key={policy.id}
                                    className="policies-table__clickable"
                                    onClick={() => navigate(`/policies/${policy.id}`)}
                                >
                                    <td>
                                        <div className="policy-identity">
                                            <span className="policy-icon">
                                                <i className="fa-solid fa-shield-halved" />
                                            </span>
                                            <span className="policy-identity__text">
                                                <strong>{policy.name}</strong>
                                                <span>{policy.description || "Sem descrição"}</span>
                                            </span>
                                        </div>
                                    </td>

                                    <td>
                                        <span className={`policy-status policy-status--${policy.status === "ACTIVE" ? "active" : "inactive"}`}>
                                            {STATUS_LABELS[policy.status] || policy.status || "—"}
                                        </span>
                                    </td>

                                    <td>{formatDate(policy.createdAt)}</td>
                                    <td>{formatDate(policy.updatedAt)}</td>

                                    <td className="policies-table__actions-col">
                                        <button
                                            type="button"
                                            className="policies-icon-btn"
                                            title="Abrir política"
                                            aria-label={`Abrir ${policy.name}`}
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                navigate(`/policies/${policy.id}`);
                                            }}
                                        >
                                            <i className="fa-solid fa-arrow-right" />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {loading && policies.length === 0 && (
                    <p className="policies-message">Carregando políticas...</p>
                )}

                {!loading && !error && filteredPolicies.length === 0 && (
                    <div className="policies-empty">
                        <i className="fa-solid fa-shield-halved" />
                        <strong>Nenhuma política encontrada</strong>
                        <span>Crie uma política para começar a definir critérios de segurança.</span>
                    </div>
                )}
            </section>
        </main>
    );
}

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";

    return new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
    }).format(date);
}
