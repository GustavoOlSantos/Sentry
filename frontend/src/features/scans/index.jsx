import { useCallback, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import "../../styles/auth.css";
import "../../styles/users.css";
import "../../styles/scans.css";

import { SideBar } from "../../components/sidebar";
import scanService from "../../services/scanService";
import { formatDateTime } from "../auth/format";
import { useAuth } from "../auth/useAuth";

import ResourceMessage from "./ResourceMessage";
import ScanStatusBadge from "./ScanStatusBadge";
import { displayDuration, groupProgress, isActive, shortId, summarizeTargets } from "./scan-utils";
import { useNow } from "./useNow";
import { useScanResource } from "./useScanResource";

const STATUS_FILTERS = [
    { value: "todos", label: "Status: Todos" },
    { value: "ativos", label: "Em andamento" },
    { value: "COMPLETED", label: "Concluídos" },
    { value: "PARTIAL", label: "Parciais" },
    { value: "FAILED", label: "Com falha" },
    { value: "NOT_FOUND", label: "Host não encontrado" },
    { value: "CANCELLED", label: "Cancelados" },
];

/** Lista de grupos de scan: cada grupo é uma chamada de "Nova varredura" (pode ter vários alvos). */
export function ScanGroups() {
    const navigate = useNavigate();
    const { can } = useAuth();

    const [search, setSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("todos");

    const load = useCallback(() => scanService.listGroups(), []);
    const { data: groups, error, loading, polling, reload } = useScanResource(
        load,
        (list) => list.some((group) => isActive(group.status))
    );
    const now = useNow(polling);

    const totals = useMemo(() => {
        const list = groups ?? [];
        return {
            all: list.length,
            running: list.filter((g) => isActive(g.status)).length,
            completed: list.filter((g) => g.status === "COMPLETED").length,
            attention: list.filter((g) => ["FAILED", "PARTIAL", "NOT_FOUND", "CANCELLED"].includes(g.status)).length,
        };
    }, [groups]);

    const filtered = useMemo(() => {
        const term = search.trim().toLowerCase();

        return (groups ?? []).filter((group) => {
            const matchesSearch =
                !term ||
                group.id.toLowerCase().includes(term) ||
                group.targets.some((target) => target.toLowerCase().includes(term));

            const matchesStatus =
                statusFilter === "todos" ||
                (statusFilter === "ativos" ? isActive(group.status) : group.status === statusFilter);

            return matchesSearch && matchesStatus;
        });
    }, [groups, search, statusFilter]);

    return (
        <main className="users-container">
            <SideBar />

            <section className="users-content">
                <div className="users-heading">
                    <div>
                        <h2>Varreduras</h2>
                        <p>
                            Grupos de scan: cada varredura disparada e os scans individuais dela.
                            {polling && <span className="scans-live"><i className="fa-solid fa-circle-notch fa-spin" /> atualizando automaticamente</span>}
                        </p>
                    </div>

                    {can("runScan") && (
                        <Link to="/novo-scan" className="users-btn users-btn--primary">
                            <i className="fa-solid fa-play" />
                            Nova varredura
                        </Link>
                    )}
                </div>

                <section className="users-overview">
                    <div className="users-overview__card">
                        <i className="fa-solid fa-layer-group" />
                        <div><h4>{totals.all}</h4><p>Grupos</p></div>
                    </div>

                    <div className="users-overview__card">
                        <i className="fa-solid fa-spinner" />
                        <div><h4>{totals.running}</h4><p>Em andamento</p></div>
                    </div>

                    <div className="users-overview__card">
                        <i className="fa-solid fa-circle-check" />
                        <div><h4>{totals.completed}</h4><p>Concluídos</p></div>
                    </div>

                    <div className="users-overview__card">
                        <i className="fa-solid fa-triangle-exclamation" />
                        <div><h4>{totals.attention}</h4><p>Com problemas</p></div>
                    </div>
                </section>

                <section className="users-filters">
                    <div className="users-filters__search">
                        <i className="fa-solid fa-magnifying-glass" />
                        <input type="text" placeholder="Buscar por alvo ou id do grupo" aria-label="Buscar grupos"
                            value={search} onChange={(e) => setSearch(e.target.value)} />
                    </div>

                    <div className="users-filters__select">
                        <select aria-label="Filtrar por status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                            {STATUS_FILTERS.map((option) => (
                                <option key={option.value} value={option.value}>{option.label}</option>
                            ))}
                        </select>
                        <i className="fa-solid fa-chevron-down" aria-hidden="true" />
                    </div>

                    <button type="button" className="users-btn users-btn--ghost" onClick={reload} disabled={loading}>
                        <i className={`fa-solid fa-rotate ${loading ? "fa-spin" : ""}`} />
                        {loading ? "Atualizando..." : "Atualizar"}
                    </button>
                </section>

                {error && groups && (
                    <p className="auth-alert auth-alert--error" role="alert">
                        <i className="fa-solid fa-circle-exclamation" />
                        Não foi possível atualizar a lista agora. Exibindo os últimos dados carregados.
                    </p>
                )}

                {groups && (
                    <div className="users-table-wrapper">
                        <table className="users-table scans-table">
                            <thead>
                                <tr>
                                    <th>Grupo</th>
                                    <th>Status</th>
                                    <th>Progresso</th>
                                    <th>Ativos</th>
                                    <th>Findings</th>
                                    <th>Criado em</th>
                                    <th>Duração</th>
                                </tr>
                            </thead>

                            <tbody>
                                {filtered.map((group) => {
                                    const progress = groupProgress(group);
                                    const targets = summarizeTargets(group.targets);

                                    return (
                                        <tr key={group.id} className="scans-row" onClick={() => navigate(`/scans/grupo/${group.id}`)}>
                                            <td>
                                                <Link to={`/scans/grupo/${group.id}`} className="scans-link" onClick={(e) => e.stopPropagation()}>
                                                    #{shortId(group.id)}
                                                </Link>

                                                <span className="scans-targets">
                                                    {targets.shown.map((target) => <code key={target}>{target}</code>)}
                                                    {targets.extra > 0 && <em>+{targets.extra}</em>}
                                                    {group.targets.length === 0 && <em>sem alvos</em>}
                                                </span>
                                            </td>

                                            <td><ScanStatusBadge status={group.status} /></td>

                                            <td>
                                                <div className="scans-progress" title={`${progress.done} de ${progress.total} scans finalizados`}>
                                                    <div className="scans-progress__bar">
                                                        <span className={`scans-progress__fill scans-progress__fill--${group.status.toLowerCase().replace("_", "-")}`}
                                                            style={{ width: `${progress.percent}%` }} />
                                                    </div>
                                                    <small>{progress.done}/{progress.total}</small>
                                                </div>
                                            </td>

                                            <td>{group.assetsCount}</td>
                                            <td>{group.findingsCount}</td>
                                            <td>{formatDateTime(group.createdAt)}</td>
                                            <td>{displayDuration(group, now)}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}

                <ResourceMessage loading={loading} error={groups ? null : error} data={groups} />

                {groups && !error && filtered.length === 0 && (
                    <p className="users-message">
                        {groups.length === 0
                            ? "Nenhuma varredura foi executada ainda."
                            : "Nenhum grupo encontrado com os filtros atuais."}
                    </p>
                )}
            </section>
        </main>
    );
}

export default ScanGroups;
