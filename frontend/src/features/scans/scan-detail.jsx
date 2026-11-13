import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";

import "../../styles/auth.css";
import "../../styles/users.css";
import "../../styles/scans.css";

import { SideBar } from "../../components/sidebar";
import scanService from "../../services/scanService";
import { formatDateTime } from "../auth/format";

import ResourceMessage from "./ResourceMessage";
import ScanStatusBadge from "./ScanStatusBadge";
import SeverityBadge from "./SeverityBadge";
import { FINDING_STATUS_LABELS, FINDING_TYPE_LABELS, displayDuration, isActive, shortId } from "./scan-utils";
import { useNow } from "./useNow";
import { useScanResource } from "./useScanResource";

/** Rota /scans/:scanId — o `key` reinicia o estado ao navegar de um scan para outro. */
export default function ScanDetailPage() {
    const { scanId } = useParams();
    return <ScanDetail key={scanId} scanId={scanId} />;
}

function ScanDetail({ scanId }) {
    const [copied, setCopied] = useState(false);

    const load = useCallback(() => scanService.getScan(scanId), [scanId]);
    const { data: scan, error, loading, polling, reload } = useScanResource(load, (detail) => isActive(detail.status));
    const now = useNow(polling);

    async function copyCommand() {
        try {
            await navigator.clipboard.writeText(scan.command);
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch {
            // sem permissão da área de transferência: o comando continua selecionável na tela
        }
    }

    return (
        <main className="users-container">
            <SideBar />

            <section className="users-content">
                <nav className="scans-breadcrumb" aria-label="Navegação">
                    <Link to="/scans">Varreduras</Link>
                    <i className="fa-solid fa-chevron-right" aria-hidden="true" />
                    {scan?.groupId && (
                        <>
                            <Link to={`/scans/grupo/${scan.groupId}`}>Grupo #{shortId(scan.groupId)}</Link>
                            <i className="fa-solid fa-chevron-right" aria-hidden="true" />
                        </>
                    )}
                    <span>{scan ? scan.target : `Scan #${shortId(scanId)}`}</span>
                </nav>

                <ResourceMessage loading={loading} error={error} data={scan}
                    notFoundText="Este scan não existe (ou foi removido)."
                    backTo="/scans" backLabel="Voltar para as varreduras" />

                {scan && (
                    <>
                        <div className="users-heading">
                            <div>
                                <h2>
                                    <code className="scans-target-title">{scan.target}</code> <ScanStatusBadge status={scan.status} />
                                </h2>
                                <p>
                                    Scan #{shortId(scan.id)}
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
                                <i className="fa-solid fa-clock" />
                                <div><h4>{displayDuration(scan, now)}</h4><p>Duração</p></div>
                            </div>

                            <div className="users-overview__card">
                                <i className="fa-solid fa-server" />
                                <div><h4>{scan.assets.length}</h4><p>Ativos encontrados</p></div>
                            </div>

                            <div className="users-overview__card">
                                <i className="fa-solid fa-bug" />
                                <div><h4>{scan.findings.length}</h4><p>Findings novos</p></div>
                            </div>

                            <div className="users-overview__card">
                                <i className="fa-solid fa-triangle-exclamation" />
                                <div><h4>{scan.warnings.length}</h4><p>Avisos</p></div>
                            </div>
                        </section>

                        <dl className="scans-meta">
                            <div><dt>Criado em</dt><dd>{formatDateTime(scan.createdAt)}</dd></div>
                            <div><dt>Iniciado em</dt><dd>{formatDateTime(scan.startedAt)}</dd></div>
                            <div><dt>Finalizado em</dt><dd>{formatDateTime(scan.finishedAt)}</dd></div>
                            <div><dt>Identificador</dt><dd><code>{scan.id}</code></dd></div>
                        </dl>

                        {scan.errorMessage && (
                            <p className="auth-alert auth-alert--error" role="alert">
                                <i className="fa-solid fa-circle-exclamation" />
                                {scan.errorMessage}
                            </p>
                        )}

                        {scan.warnings.length > 0 && (
                            <div className="scans-warnings" role="status">
                                <strong><i className="fa-solid fa-triangle-exclamation" /> Avisos da execução</strong>
                                <ul>
                                    {scan.warnings.map((warning, index) => <li key={index}>{warning}</li>)}
                                </ul>
                            </div>
                        )}

                        {scan.command && (
                            <>
                                <h3 className="scans-section-title">Comando executado</h3>

                                <div className="scans-command">
                                    <pre><code>{scan.command}</code></pre>

                                    <button type="button" className="users-btn users-btn--ghost" onClick={copyCommand}>
                                        <i className={`fa-solid ${copied ? "fa-check" : "fa-copy"}`} />
                                        {copied ? "Copiado" : "Copiar"}
                                    </button>
                                </div>
                            </>
                        )}

                        <h3 className="scans-section-title">Ativos encontrados</h3>

                        {scan.assets.length > 0 ? (
                            <div className="users-table-wrapper">
                                <table className="users-table">
                                    <thead>
                                        <tr>
                                            <th>Endereço IP</th>
                                            <th>Hostname</th>
                                            <th>Sistema operacional</th>
                                            <th>Portas abertas</th>
                                            <th>Descoberto em</th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        {scan.assets.map((asset) => (
                                            <tr key={asset.id}>
                                                <td><code>{asset.ipAddress}</code></td>
                                                <td>{asset.hostname || "—"}</td>
                                                <td>{asset.operatingSystem || "—"}</td>
                                                <td>
                                                    <span className="scans-ports">
                                                        {asset.openPorts.map((port) => (
                                                            <span key={`${port.port}/${port.protocol}`} className="scans-port"
                                                                title={[port.product, port.version].filter(Boolean).join(" ") || undefined}>
                                                                {port.port}/{port.protocol}{port.service ? ` ${port.service}` : ""}
                                                            </span>
                                                        ))}
                                                        {asset.openPorts.length === 0 && "—"}
                                                    </span>
                                                </td>
                                                <td>{formatDateTime(asset.discoveredAt)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p className="users-message">
                                {isActive(scan.status) ? "A varredura ainda está em andamento." : "Nenhum ativo respondeu a este scan."}
                            </p>
                        )}

                        <h3 className="scans-section-title">Findings detectados por este scan</h3>

                        {scan.findings.length > 0 ? (
                            <div className="users-table-wrapper">
                                <table className="users-table">
                                    <thead>
                                        <tr>
                                            <th>Severidade</th>
                                            <th>Finding</th>
                                            <th>Ativo</th>
                                            <th>Tipo</th>
                                            <th>Status</th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        {scan.findings.map((finding) => (
                                            <tr key={finding.id}>
                                                <td><SeverityBadge severity={finding.severity} /></td>
                                                <td>
                                                    <span className="scans-finding-title">{finding.title}</span>
                                                    <small className="scans-finding-code">{finding.code}</small>
                                                </td>
                                                <td>
                                                    <code>{finding.assetIp}{finding.port ? `:${finding.port}` : ""}</code>
                                                </td>
                                                <td>{FINDING_TYPE_LABELS[finding.type] ?? finding.type}</td>
                                                <td>{FINDING_STATUS_LABELS[finding.status] ?? finding.status}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p className="users-message">Este scan não detectou nenhum finding novo.</p>
                        )}

                        <p className="scans-footnote">
                            Aqui aparecem só os findings que este scan detectou pela primeira vez; o que já existia
                            e foi reencontrado continua no <Link to="/board">board</Link>.
                        </p>
                    </>
                )}
            </section>
        </main>
    );
}
