import { useCallback } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import "../../styles/auth.css";
import "../../styles/users.css";
import "../../styles/scans.css";

import { SideBar } from "../../components/sidebar";
import scanService from "../../services/scanService";
import { formatDateTime } from "../auth/format";

import ResourceMessage from "./ResourceMessage";
import ScanStatusBadge from "./ScanStatusBadge";
import { describeStatusCounts, displayDuration, groupProgress, isActive, shortId } from "./scan-utils";
import { useNow } from "./useNow";
import { useScanResource } from "./useScanResource";

/** Rota /scans/grupo/:groupId — o `key` reinicia o estado ao navegar de um grupo para outro. */
export default function GroupDetailPage() {
    const { groupId } = useParams();
    return <GroupDetail key={groupId} groupId={groupId} />;
}

function GroupDetail({ groupId }) {
    const navigate = useNavigate();

    const load = useCallback(() => scanService.getGroup(groupId), [groupId]);
    const { data, error, loading, polling, reload } = useScanResource(
        load,
        (detail) => isActive(detail.group.status)
    );
    const now = useNow(polling);

    const group = data?.group;
    const progress = group ? groupProgress(group) : null;

    return (
        <main className="users-container">
            <SideBar />

            <section className="users-content">
                <nav className="scans-breadcrumb" aria-label="Navegação">
                    <Link to="/scans">Varreduras</Link>
                    <i className="fa-solid fa-chevron-right" aria-hidden="true" />
                    <span>Grupo #{shortId(groupId)}</span>
                </nav>

                <ResourceMessage loading={loading} error={error} data={data}
                    notFoundText="Este grupo de varredura não existe (ou foi removido)."
                    backTo="/scans" backLabel="Voltar para as varreduras" />

                {group && (
                    <>
                        <div className="users-heading">
                            <div>
                                <h2>
                                    Grupo #{shortId(group.id)} <ScanStatusBadge status={group.status} />
                                </h2>
                                <p>
                                    {describeStatusCounts(group.scansByStatus) || "Sem scans neste grupo."}
                                    {polling && <span className="scans-live"><i className="fa-solid fa-circle-notch fa-spin" /> atualizando automaticamente</span>}
                                </p>
                            </div>

                            <button type="button" className="users-btn users-btn--ghost" onClick={reload} disabled={loading}>
                                <i className={`fa-solid fa-rotate ${loading ? "fa-spin" : ""}`} />
                                Atualizar
                            </button>
                        </div>

                        <section className="users-overview">
                            <div className="users-overview__card">
                                <i className="fa-solid fa-bullseye" />
                                <div><h4>{progress.done}/{progress.total}</h4><p>Scans finalizados</p></div>
                            </div>

                            <div className="users-overview__card">
                                <i className="fa-solid fa-clock" />
                                <div><h4>{displayDuration(group, now)}</h4><p>Duração</p></div>
                            </div>

                            <div className="users-overview__card">
                                <i className="fa-solid fa-server" />
                                <div><h4>{group.assetsCount}</h4><p>Ativos encontrados</p></div>
                            </div>

                            <div className="users-overview__card">
                                <i className="fa-solid fa-bug" />
                                <div><h4>{group.findingsCount}</h4><p>Findings novos</p></div>
                            </div>
                        </section>

                        <dl className="scans-meta">
                            <div><dt>Criado em</dt><dd>{formatDateTime(group.createdAt)}</dd></div>
                            <div><dt>Iniciado em</dt><dd>{formatDateTime(group.startedAt)}</dd></div>
                            <div><dt>Finalizado em</dt><dd>{formatDateTime(group.finishedAt)}</dd></div>
                            <div><dt>Identificador</dt><dd><code>{group.id}</code></dd></div>
                        </dl>

                        {group.errorMessage && (
                            <p className="auth-alert auth-alert--error" role="alert">
                                <i className="fa-solid fa-circle-exclamation" />
                                {group.errorMessage}
                            </p>
                        )}

                        <h3 className="scans-section-title">Scans do grupo</h3>

                        <div className="users-table-wrapper">
                            <table className="users-table scans-table">
                                <thead>
                                    <tr>
                                        <th>Alvo</th>
                                        <th>Status</th>
                                        <th>Início</th>
                                        <th>Duração</th>
                                        <th>Ativos</th>
                                        <th>Findings</th>
                                        <th>Avisos</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {data.scans.map((scan) => (
                                        <tr key={scan.id} className="scans-row" onClick={() => navigate(`/scans/${scan.id}`)}>
                                            <td>
                                                <Link to={`/scans/${scan.id}`} className="scans-link" onClick={(e) => e.stopPropagation()}>
                                                    <code>{scan.target}</code>
                                                </Link>
                                                {scan.errorMessage && <span className="scans-inline-error">{scan.errorMessage}</span>}
                                            </td>
                                            <td><ScanStatusBadge status={scan.status} /></td>
                                            <td>{formatDateTime(scan.startedAt)}</td>
                                            <td>{displayDuration(scan, now)}</td>
                                            <td>{scan.assetsCount}</td>
                                            <td>{scan.findingsCount}</td>
                                            <td>
                                                {scan.warningsCount > 0
                                                    ? <span className="scans-warning-count"><i className="fa-solid fa-triangle-exclamation" /> {scan.warningsCount}</span>
                                                    : "—"}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {data.scans.length === 0 && <p className="users-message">Este grupo ainda não tem scans.</p>}
                    </>
                )}
            </section>
        </main>
    );
}
