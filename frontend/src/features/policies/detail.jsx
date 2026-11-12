import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { SideBar } from "../../components/sidebar";
import { useAuth } from "../auth/useAuth";
import policyService from "../../services/policyService";
import PolicyFormModal from "./policy-form-modal";
import RuleFormModal from "./rule-form-modal";
import "../../styles/policies.css";

const TYPE_LABELS = {
    PORT_RESTRICTION: "Restrição de porta",
    FORBIDDEN_SERVICE: "Serviço proibido",
};

const SEVERITY_LABELS = {
    LOW: "Baixa",
    MEDIUM: "Média",
    HIGH: "Alta",
    CRITICAL: "Crítica",
};

function getApiErrorMessage(error, fallback) {
    return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
}

export default function PolicyDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { can } = useAuth();

    const [policy, setPolicy] = useState(null);
    const [rules, setRules] = useState([]);
    const [search, setSearch] = useState("");
    const [typeFilter, setTypeFilter] = useState("todos");
    const [enabledFilter, setEnabledFilter] = useState("todos");

    const [loading, setLoading] = useState(true);
    const [rulesLoading, setRulesLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const [policyModal, setPolicyModal] = useState(undefined);
    const [ruleModal, setRuleModal] = useState(null);
    const [deleteRuleTarget, setDeleteRuleTarget] = useState(null);

    const loadPolicy = async () => {
        setLoading(true);
        try {
            const data = await policyService.get(id);
            setPolicy(data);
            setError("");
        } catch (err) {
            console.error("Falha ao carregar política", err);
            if (err?.response?.status === 404) {
                setError("A política solicitada não foi encontrada.");
            } else {
                setError(getApiErrorMessage(err, "Não foi possível carregar a política."));
            }
        } finally {
            setLoading(false);
        }
    };

    const loadRules = async () => {
        setRulesLoading(true);
        try {
            const data = await policyService.listRules();
            const allRules = Array.isArray(data) ? data : [];
            setRules(allRules.filter((rule) => String(rule.policy?.id) === String(id)));
        } catch (err) {
            console.error("Falha ao carregar regras", err);
            setError(getApiErrorMessage(err, "Não foi possível carregar as regras da política."));
        } finally {
            setRulesLoading(false);
        }
    };

    useEffect(() => {
        loadPolicy();
        loadRules();
    }, [id]);

    const filteredRules = useMemo(() => {
        const term = search.trim().toLowerCase();

        return rules.filter((rule) => {
            const matchesSearch =
                (rule.name || "").toLowerCase().includes(term) ||
                (rule.description || "").toLowerCase().includes(term) ||
                (rule.expectedValue || "").toLowerCase().includes(term);

            const matchesType = typeFilter === "todos" || rule.type === typeFilter;

            const matchesEnabled =
                enabledFilter === "todos" ||
                (enabledFilter === "enabled" ? rule.enabled !== false : rule.enabled === false);

            return matchesSearch && matchesType && matchesEnabled;
        });
    }, [rules, search, typeFilter, enabledFilter]);

    const ruleTotals = useMemo(() => ({
        all: rules.length,
        enabled: rules.filter((rule) => rule.enabled !== false).length,
        disabled: rules.filter((rule) => rule.enabled === false).length,
    }), [rules]);

    const handlePolicySaved = (saved, message) => {
        setPolicy(saved);
        setPolicyModal(undefined);
        setNotice(message);
    };

    const handleRuleSaved = (saved, message) => {
        setRuleModal(null);
        setNotice(message);
        loadRules();
    };

    const handleDeleteRule = async () => {
        if (!deleteRuleTarget) return;

        try {
            await policyService.removeRule(deleteRuleTarget.id);
            setNotice("Regra excluída.");
            setDeleteRuleTarget(null);
            loadRules();
        } catch (err) {
            setDeleteRuleTarget(null);
            setError(getApiErrorMessage(err, "Não foi possível excluir a regra."));
        }
    };

    const handleDeletePolicy = async () => {
        if (!policy) return;

        if (rules.length > 0) {
            setError("Remova as regras desta política antes de excluí-la.");
            return;
        }

        if (!window.confirm(`Excluir a política "${policy.name}"? Esta ação não pode ser desfeita.`)) {
            return;
        }

        try {
            await policyService.remove(policy.id);
            navigate("/policies", { replace: true });
        } catch (err) {
            setError(getApiErrorMessage(err, "Não foi possível excluir a política."));
        }
    };

    if (loading) {
        return (
            <main className="policies-container">
                <SideBar />
                <section className="policies-content">
                    <p className="policies-message">Carregando política...</p>
                </section>
            </main>
        );
    }

    if (!policy) {
        return (
            <main className="policies-container">
                <SideBar />
                <section className="policies-content">
                    <div className="policies-notice policies-notice--error">
                        <i className="fa-solid fa-circle-exclamation" />
                        <span>{error || "Política não encontrada."}</span>
                    </div>
                    <button type="button" className="policies-btn policies-btn--ghost" onClick={() => navigate("/policies")}>
                        <i className="fa-solid fa-arrow-left" />
                        Voltar para políticas
                    </button>
                </section>
            </main>
        );
    }

    return (
        <main className="policies-container">
            <SideBar />

            <section className="policies-content">
                <div className="policy-detail-breadcrumb">
                    <Link to="/policies">
                        <i className="fa-solid fa-arrow-left" />
                        Políticas
                    </Link>
                    <span>/</span>
                    <strong>{policy.name}</strong>
                </div>

                <header className="policy-detail-header">
                    <div className="policy-detail-title">
                        <span className="policy-detail-icon">
                            <i className="fa-solid fa-shield-halved" />
                        </span>
                        <div>
                            <div className="policy-detail-title__top">
                                <h2>{policy.name}</h2>
                                <span className={`policy-status policy-status--${policy.status === "ACTIVE" ? "active" : "inactive"}`}>
                                    {policy.status === "ACTIVE" ? "Ativa" : "Inativa"}
                                </span>
                            </div>
                            <p>{policy.description || "Sem descrição cadastrada."}</p>
                        </div>
                    </div>

                    {can("writePolicies") && (
                        <div className="policy-detail-actions">
                            <button type="button" className="policies-btn policies-btn--ghost" onClick={() => setPolicyModal(policy)}>
                                <i className="fa-solid fa-pen" />
                                Editar
                            </button>
                            <button type="button" className="policies-btn policies-btn--danger" onClick={handleDeletePolicy}>
                                <i className="fa-solid fa-trash-can" />
                                Excluir
                            </button>
                        </div>
                    )}
                </header>

                {error && (
                    <div className="policies-notice policies-notice--error" role="alert">
                        <i className="fa-solid fa-circle-exclamation" />
                        <span>{error}</span>
                        <button type="button" onClick={() => setError("")} aria-label="Fechar aviso">&times;</button>
                    </div>
                )}

                {notice && (
                    <div className="policies-notice policies-notice--success" role="status">
                        <i className="fa-solid fa-circle-check" />
                        <span>{notice}</span>
                        <button type="button" onClick={() => setNotice("")} aria-label="Fechar aviso">&times;</button>
                    </div>
                )}

                <section className="policy-meta">
                    <div>
                        <span>Criada em</span>
                        <strong>{formatDate(policy.createdAt)}</strong>
                    </div>
                    <div>
                        <span>Última atualização</span>
                        <strong>{formatDate(policy.updatedAt)}</strong>
                    </div>
                    <div>
                        <span>Regras</span>
                        <strong>{ruleTotals.all}</strong>
                    </div>
                    <div>
                        <span>Regras habilitadas</span>
                        <strong>{ruleTotals.enabled}</strong>
                    </div>
                </section>

                <section className="policy-rules-section">
                    <div className="policy-section-heading">
                        <div>
                            <h3>Regras da política</h3>
                            <p>Estas regras são avaliadas contra os serviços descobertos durante as varreduras.</p>
                        </div>

                        {can("writePolicies") && (
                            <button
                                type="button"
                                className="policies-btn policies-btn--primary"
                                onClick={() => setRuleModal({ mode: "create" })}
                            >
                                <i className="fa-solid fa-plus" />
                                Nova regra
                            </button>
                        )}
                    </div>

                    <div className="policy-rule-filters">
                        <div className="policies-filters__search">
                            <i className="fa-solid fa-magnifying-glass" />
                            <input
                                type="text"
                                placeholder="Buscar regras"
                                value={search}
                                onChange={(event) => setSearch(event.target.value)}
                                aria-label="Buscar regras"
                            />
                        </div>

                        <div className="policies-filters__select">
                            <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} aria-label="Filtrar por tipo">
                                <option value="todos">Tipo: Todos</option>
                                <option value="PORT_RESTRICTION">Restrição de porta</option>
                                <option value="FORBIDDEN_SERVICE">Serviço proibido</option>
                            </select>
                            <i className="fa-solid fa-chevron-down" aria-hidden="true" />
                        </div>

                        <div className="policies-filters__select">
                            <select value={enabledFilter} onChange={(event) => setEnabledFilter(event.target.value)} aria-label="Filtrar por status">
                                <option value="todos">Status: Todos</option>
                                <option value="enabled">Habilitadas</option>
                                <option value="disabled">Desabilitadas</option>
                            </select>
                            <i className="fa-solid fa-chevron-down" aria-hidden="true" />
                        </div>

                        <button type="button" className="policies-btn policies-btn--ghost" onClick={loadRules} disabled={rulesLoading}>
                            <i className={`fa-solid fa-rotate ${rulesLoading ? "fa-spin" : ""}`} />
                            Atualizar
                        </button>
                    </div>

                    <div className="policy-rules-table-wrapper">
                        <table className="policy-rules-table">
                            <thead>
                                <tr>
                                    <th>Regra</th>
                                    <th>Tipo</th>
                                    <th>Critério</th>
                                    <th>Severidade</th>
                                    <th>Status</th>
                                    {can("writePolicies") && <th className="policies-table__actions-col">Ações</th>}
                                </tr>
                            </thead>
                            <tbody>
                                {filteredRules.map((rule) => (
                                    <tr key={rule.id}>
                                        <td>
                                            <div className="rule-identity">
                                                <strong>{rule.name}</strong>
                                                <span>{rule.description || "Sem descrição"}</span>
                                            </div>
                                        </td>
                                        <td>
                                            <span className="rule-type">
                                                {TYPE_LABELS[rule.type] || rule.type || "—"}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="rule-criterion">
                                                <code>{rule.expectedValue || "—"}</code>
                                                {rule.condition && <span>{rule.condition.toUpperCase()}</span>}
                                            </div>
                                        </td>
                                        <td>
                                            <span className={`rule-severity rule-severity--${(rule.severity || "MEDIUM").toLowerCase()}`}>
                                                {SEVERITY_LABELS[rule.severity] || rule.severity || "Média"}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`policy-rule-status ${rule.enabled !== false ? "enabled" : "disabled"}`}>
                                                {rule.enabled !== false ? "Habilitada" : "Desabilitada"}
                                            </span>
                                        </td>
                                        {can("writePolicies") && (
                                            <td className="policies-table__actions-col">
                                                <div className="policies-row-actions">
                                                    <button
                                                        type="button"
                                                        className="policies-icon-btn"
                                                        title="Editar regra"
                                                        onClick={() => setRuleModal(rule)}
                                                    >
                                                        <i className="fa-solid fa-pen" />
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="policies-icon-btn policies-icon-btn--danger"
                                                        title="Excluir regra"
                                                        onClick={() => setDeleteRuleTarget(rule)}
                                                    >
                                                        <i className="fa-solid fa-trash-can" />
                                                    </button>
                                                </div>
                                            </td>
                                        )}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {rulesLoading && rules.length === 0 && (
                        <p className="policies-message">Carregando regras...</p>
                    )}

                    {!rulesLoading && filteredRules.length === 0 && (
                        <div className="policies-empty">
                            <i className="fa-solid fa-list-check" />
                            <strong>Nenhuma regra encontrada</strong>
                            <span>
                                {rules.length === 0
                                    ? "Adicione a primeira regra desta política."
                                    : "Nenhuma regra corresponde aos filtros atuais."}
                            </span>
                        </div>
                    )}
                </section>
            </section>

            {can("writePolicies") && (
                <>
                    <PolicyFormModal
                        policy={policyModal}
                        onClose={() => setPolicyModal(undefined)}
                        onSaved={handlePolicySaved}
                    />

                    {ruleModal && (
                        <RuleFormModal
                            policy={policy}
                            rule={ruleModal.mode === "create" ? null : ruleModal}
                            onClose={() => setRuleModal(null)}
                            onSaved={handleRuleSaved}
                        />
                    )}
                </>
            )}

            {deleteRuleTarget && (
                <div className="policy-modal-backdrop" onClick={() => setDeleteRuleTarget(null)}>
                    <div className="policy-modal policy-modal--confirm" onClick={(event) => event.stopPropagation()}>
                        <header className="policy-modal__header">
                            <div>
                                <span className="policy-modal__eyebrow policy-modal__eyebrow--danger">
                                    <i className="fa-solid fa-triangle-exclamation" />
                                    Atenção
                                </span>
                                <h3>Excluir regra?</h3>
                            </div>
                            <button type="button" onClick={() => setDeleteRuleTarget(null)} aria-label="Fechar">&times;</button>
                        </header>
                        <p className="policy-confirm-text">
                            A regra <strong>{deleteRuleTarget.name}</strong> será removida permanentemente.
                        </p>
                        <footer className="policy-form__actions">
                            <button type="button" className="policies-btn policies-btn--ghost" onClick={() => setDeleteRuleTarget(null)}>
                                Cancelar
                            </button>
                            <button type="button" className="policies-btn policies-btn--danger" onClick={handleDeleteRule}>
                                <i className="fa-solid fa-trash-can" />
                                Excluir regra
                            </button>
                        </footer>
                    </div>
                </div>
            )}
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
