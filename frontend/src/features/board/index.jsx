import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import api from '../../services/api';
import { SideBar } from "../../components/sidebar";
import '../../styles/board.css';

const FINDING_COLUMNS = [
    { id: 'open',     title: 'ABERTO',       statuses: ['OPEN'],                             dropStatus: 'OPEN',        userDroppable: true },
    { id: 'progress', title: 'EM ANDAMENTO', statuses: ['IN_PROGRESS', 'FAILED_VALIDATION'], dropStatus: 'IN_PROGRESS', userDroppable: true },
    { id: 'resolved', title: 'RESOLVIDA',    statuses: ['RESOLVED'],                         dropStatus: 'RESOLVED',    userDroppable: true },
    { id: 'closed',   title: 'FECHADA',      statuses: ['JUSTIFIED', 'VALIDATED'],           dropStatus: 'VALIDATED',   userDroppable: false } // só o sistema move pra cá
];

const STATUS_LABELS = {
    OPEN: 'Aberto',
    IN_PROGRESS: 'Executando',
    FAILED_VALIDATION: 'Inválida',
    RESOLVED: 'Resolvida',
    JUSTIFIED: 'Justificada',
    VALIDATED: 'Validada'
};

const TYPE_LABELS = {
    VULNERABILITY: 'Vulner.',
    POLICY_VIOLATION: 'Política'
};

// Status que o usuário pode escolher manualmente (VALIDATED e FAILED_VALIDATION são do sistema)
const MANUAL_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'JUSTIFIED'];

// TODO: trocar por GET /users quando o cadastro de usuários existir
const MOCK_ASSIGNEES = [
    { id: '1', name: 'Gustavo Santos' },
    { id: '2', name: 'Leandro Lucas' },
    { id: '3', name: 'Sophia Trotte' },
    { id: '4', name: 'Vivian Motta' }
];

const formatDate = (value) =>
    value ? new Date(value).toLocaleString('pt-BR') : '—';

// ---------- Formulário de gerenciamento (drawer) ----------
function FindingManageForm({ finding, onChangeStatus, onChangeAssignee }) {

    const [status, setStatus] = useState(finding.status);
    const [justification, setJustification] = useState(finding.justification ?? '');
    const [submitting, setSubmitting] = useState(false);
    const [formError, setFormError] = useState(null);

    const needsJustification = status === 'JUSTIFIED';
    const dirty = status !== finding.status || (needsJustification && justification.trim() !== (finding.justification ?? '').trim());

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (needsJustification && !justification.trim()) {
            setFormError('Informe a justificativa para marcar como Justificada.');
            return;
        }

        setFormError(null);
        setSubmitting(true);

        const ok = await onChangeStatus(
            finding,
            status,
            needsJustification ? justification.trim() : undefined
        );

        setSubmitting(false);

        if (!ok) setStatus(finding.status);
    };

    return (
        <section className="drawer-section drawer-manage">
            <h5>Gerenciar</h5>

            <label className="manage-field">
                <span>Responsável</span>
                <select
                    value={finding.assigneeId ?? ''}
                    onChange={(e) => onChangeAssignee(finding.id, e.target.value || null)}
                >
                    <option value="">Sem responsável</option>
                    {MOCK_ASSIGNEES.map(user => (
                        <option key={user.id} value={user.id}>{user.name}</option>
                    ))}
                </select>
            </label>

            <form onSubmit={handleSubmit}>
                <label className="manage-field">
                    <span>Status</span>
                    <select
                        value={status}
                        onChange={(e) => {
                            setStatus(e.target.value);
                            setFormError(null);
                        }}
                        disabled={submitting}
                    >
                        {/* se o status atual é do sistema, mostra ele desabilitado */}
                        {!MANUAL_STATUSES.includes(finding.status) && (
                            <option value={finding.status} disabled>
                                {STATUS_LABELS[finding.status] || finding.status}
                            </option>
                        )}
                        {MANUAL_STATUSES.map(s => (
                            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                        ))}
                    </select>
                </label>

                {needsJustification && (
                    <label className="manage-field">
                        <span>Justificativa</span>
                        <textarea
                            rows={3}
                            value={justification}
                            onChange={(e) => setJustification(e.target.value)}
                            placeholder="Explique por que este finding é aceito/justificado..."
                            disabled={submitting}
                        />
                    </label>
                )}

                {formError && <p className="manage-error">{formError}</p>}

                <button type="submit" className="btn-update" disabled={!dirty || submitting}>
                    {submitting ? 'Enviando...' : 'Enviar'}
                </button>
            </form>
        </section>
    );
}

export default function Board() {
    const [findings, setFindings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const [draggedId, setDraggedId] = useState(null);
    const [dragOverColumn, setDragOverColumn] = useState(null);
    const [selectedId, setSelectedId] = useState(null);

    const [warning, setWarning] = useState(null);
    const lockedHoverRef = useRef(null);
    const warningTimerRef = useRef(null);

    const showWarning = useCallback((message) => {
        setWarning(message);
        clearTimeout(warningTimerRef.current);
        warningTimerRef.current = setTimeout(() => setWarning(null), 4000);
    }, []);

    useEffect(() => () => clearTimeout(warningTimerRef.current), []);

    const fetchFindings = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);

            const response = await api.get('/findings');
            const data = Array.isArray(response.data)
                ? response.data
                : response.data?.content ?? [];

            setFindings(data);
        } catch (err) {
            console.error('Falha ao buscar findings:', err);
            setError('Não foi possível carregar os findings.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchFindings();
    }, [fetchFindings]);

    const groupedFindings = useMemo(() => {
        return FINDING_COLUMNS.map(column => ({
            ...column,
            findings: findings.filter(f => column.statuses.includes(f.status))
        }));
    }, [findings]);

    const selectedFinding = useMemo(
        () => findings.find(f => f.id === selectedId) ?? null,
        [findings, selectedId]
    );

    // ---------- Atualização de status / assignee ----------
    const updateFindingLocal = (id, patch) =>
        setFindings(prev => prev.map(f => (f.id === id ? { ...f, ...patch } : f)));

    // Usado tanto pelo drag-and-drop quanto pelo formulário do drawer.
    // Retorna true/false para o chamador saber se deu certo.
    const changeStatus = async (finding, newStatus, justification) => {
        const previous = {
            status: finding.status,
            justification: finding.justification
        };

        updateFindingLocal(finding.id, {
            status: newStatus,
            ...(justification !== undefined && { justification })
        });
        setError(null);

        try {
            await api.post(`/findings/${finding.id}/status`, {
                status: newStatus,
                justification
            });
            return true;
        } catch (err) {
            console.error('Falha ao atualizar status:', err);
            updateFindingLocal(finding.id, previous);
            setError('Não foi possível atualizar o finding. A alteração foi desfeita.');
            return false;
        }
    };

    const moveFinding = (finding, column) => {
        if (column.statuses.includes(finding.status)) return;
        changeStatus(finding, column.dropStatus);
    };

    // Mock: só em memória até existir endpoint
    const handleAssigneeChange = (id, assigneeId) => {
        updateFindingLocal(id, { assigneeId });
        // TODO: api.patch(`/findings/${id}/assignee`, { assigneeId })
    };

    // ---------- Drag and drop ----------
    const handleDragStart = (event, finding) => {
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', String(finding.id)); // necessário no Firefox
        setDraggedId(finding.id);
    };

    const handleDragEnd = () => {
        const droppedOnLocked = lockedHoverRef.current;
        lockedHoverRef.current = null;

        setDraggedId(null);
        setDragOverColumn(null);

        if (droppedOnLocked) {
            showWarning(
                'A coluna "Fechada" é preenchida automaticamente pelo sistema após a validação dos findings em "Resolvida". Não é possível mover cards para ela manualmente.'
            );
        }
    };

    const handleDragOver = (event, column) => {
        if (!column.userDroppable) {
            event.dataTransfer.dropEffect = 'none';

            const dragged = findings.find(f => f.id === draggedId);
            const isOwnColumn = dragged && column.statuses.includes(dragged.status);
            lockedHoverRef.current = isOwnColumn ? null : column.id;

            if (dragOverColumn) setDragOverColumn(null);
            return;
        }

        lockedHoverRef.current = null;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
        if (dragOverColumn !== column.id) setDragOverColumn(column.id);
    };

    const handleDragLeave = (event) => {
        const next = event.relatedTarget;

        if (!next) return;

        if (!event.currentTarget.contains(next)) {
            lockedHoverRef.current = null;
            setDragOverColumn(null);
        }
    };

    const handleDrop = (event, column) => {
        event.preventDefault();
        const finding = findings.find(f => f.id === draggedId);
        handleDragEnd();

        if (!column.userDroppable) return; 
        if (finding) moveFinding(finding, column);
    };

    // ---------- Drawer ----------
    const closeDrawer = useCallback(() => setSelectedId(null), []);

    useEffect(() => {
        if (!selectedId) return;
        const onKeyDown = (e) => e.key === 'Escape' && closeDrawer();
        window.addEventListener('keydown', onKeyDown);
        return () => window.removeEventListener('keydown', onKeyDown);
    }, [selectedId, closeDrawer]);

    return (
        <main className="findings-page">
            <SideBar />

            <section className="findings-content">
                <header className="findings-header">
                    <div>
                        <h2>Findings</h2>
                        <p>Gerencie e acompanhe as ocorrências identificadas no ambiente.</p>
                    </div>

                    <button className="btn-update" onClick={fetchFindings} disabled={loading} type="button">
                        <i className={`fa-solid fa-rotate ${loading ? 'fa-spin' : ''}`}></i>
                        {loading ? 'Atualizando...' : 'Atualizar'}
                    </button>
                </header>

                {error && (
                    <div className="findings-error">
                        <i className="fa-solid fa-circle-exclamation"></i>
                        {error}
                    </div>
                )}

                {warning && (
                    <div className="findings-warning" role="alert">
                        <i className="fa-solid fa-lock"></i>
                        <span>{warning}</span>
                        <button type="button" onClick={() => setWarning(null)} aria-label="Fechar aviso">
                            <i className="fa-solid fa-xmark"></i>
                        </button>
                    </div>
                )}

                {loading && findings.length === 0 ? (
                    <div className="findings-empty">
                        <i className="fa-solid fa-spinner fa-spin"></i>
                        <p>Carregando findings...</p>
                    </div>
                ) : (
                    <section className="kanban-board">
                        {groupedFindings.map(column => (
                            <article
                                className={[
                                    'kanban-column',
                                    dragOverColumn === column.id ? 'drag-over' : '',
                                    !column.userDroppable && draggedId ? 'drop-locked' : ''
                                ].join(' ')}
                                key={column.id}
                                onDragOver={(e) => handleDragOver(e, column)}
                                onDragLeave={handleDragLeave}
                                onDrop={(e) => handleDrop(e, column)}
                            >
                                <header className="kanban-column-header">
                                    <div className="kanban-column-title">
                                        <span className={`kanban-column-dot ${column.id}`}>•</span>
                                        <h3>{column.title}</h3>
                                    </div>
                                    <span>{column.findings.length}</span>
                                </header>

                                <div className="kanban-cards">
                                    {column.findings.length > 0 ? (
                                        column.findings.map(finding => (
                                            <article
                                                className={`finding-card ${draggedId === finding.id ? 'dragging' : ''}`}
                                                key={finding.id}
                                                draggable
                                                onDragStart={(e) => handleDragStart(e, finding)}
                                                onDragEnd={handleDragEnd}
                                                onClick={() => setSelectedId(finding.id)}
                                            >
                                                <h4>{finding.title || 'Finding sem título'} - {finding?.asset?.ipAddress}</h4>
                                                <p>{finding.description || 'Finding sem descrição'}</p>

                                                <div className="info-container">
                                                    <div className="finding-card-info">
                                                        <span className="finding-label">Status</span>
                                                        <span className={`finding-status status-${finding.status?.toLowerCase()}`}>
                                                            {STATUS_LABELS[finding.status] || finding.status}
                                                        </span>
                                                    </div>

                                                    <div className="finding-card-info">
                                                        <span className="finding-label">Tipo</span>
                                                        <span className={`finding-type type-${finding.type?.toLowerCase()}`}>
                                                            {TYPE_LABELS[finding.type] || finding.type}
                                                        </span>
                                                    </div>

                                                    <div className="finding-card-info">
                                                        <span className="finding-label">Severidade</span>
                                                        <span className={`finding-severity severity-${finding.severity?.toLowerCase()}`}>
                                                            {finding.severity || 'N/A'}
                                                        </span>
                                                    </div>
                                                </div>
                                            </article>
                                        ))
                                    ) : (
                                        <div className="kanban-empty">
                                            <i className="fa-regular fa-folder-open"></i>
                                            <p>Nenhum finding nesta etapa.</p>
                                        </div>
                                    )}
                                </div>
                            </article>
                        ))}
                    </section>
                )}

                <div className={`drawer-overlay ${selectedFinding ? 'open' : ''}`} onClick={closeDrawer} />

                <aside className={`finding-drawer ${selectedFinding ? 'open' : ''}`} aria-hidden={!selectedFinding}>
                    {selectedFinding && (
                        <>
                            <header className="drawer-header">
                                <h3>{selectedFinding.title || 'Finding sem título'}</h3>
                                <button type="button" className="drawer-close" onClick={closeDrawer} aria-label="Fechar">
                                    <i className="fa-solid fa-xmark"></i>
                                </button>
                            </header>

                            <div className="drawer-manage-container">
                                <aside>
                                    <FindingManageForm
                                        key={selectedFinding.id}
                                        finding={selectedFinding}
                                        onChangeStatus={changeStatus}
                                        onChangeAssignee={handleAssigneeChange}
                                    />
                                </aside>

                                <div className="drawer-body">
                                    <section className="drawer-section">
                                        <h5>Descrição</h5>
                                        <p>{selectedFinding.description || 'Sem descrição.'}</p>

                                        <div className="drawer-dates">
                                            <h5>Criado em: </h5>
                                            <p>{formatDate(selectedFinding.detectedAt)}</p>
                                        </div>

                                        <div className="drawer-dates">
                                            <h5>Resolvido em: </h5>
                                            <p>{formatDate(selectedFinding.resolvedAt)}</p>
                                        </div>
                                    </section>

                                    <div className="drawer-badges">
                                        <span className={`finding-status status-${selectedFinding.status?.toLowerCase()}`}>
                                            {STATUS_LABELS[selectedFinding.status] || selectedFinding.status}
                                        </span>

                                        <span className={`finding-type type-${selectedFinding.type?.toLowerCase()}`}>
                                            {TYPE_LABELS[selectedFinding.type] || selectedFinding.type}
                                        </span>

                                        <span className={`finding-severity severity-${selectedFinding.severity?.toLowerCase()}`}>
                                            {selectedFinding.severity || 'N/A'}
                                        </span>
                                    </div>

                                    <section className="drawer-section details">
                                        <h5>Detalhes</h5>

                                        <table>
                                            <thead>
                                                <tr>
                                                    <th>IP</th>
                                                    <th>Porta</th>
                                                    <th>Protocolo</th>
                                                    <th>Serviço</th>
                                                    <th>Produto</th>
                                                    <th>Versão</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td>{selectedFinding.asset?.ipAddress ?? '—'}</td>
                                                    <td>{selectedFinding.service?.port ?? '—'}</td>
                                                    <td>{selectedFinding.service?.protocol ?? '—'}</td>
                                                    <td>{selectedFinding.service?.serviceName ?? '—'}</td>
                                                    <td>{selectedFinding.service?.product ?? '—'}</td>
                                                    <td>{selectedFinding.service?.version ?? '—'}</td>
                                                </tr>
                                            </tbody>
                                        </table>

                                        <p><strong>hostname:</strong> {selectedFinding.asset?.hostname ?? '—'}</p>
                                        <p><strong>MAC:</strong> {selectedFinding.asset?.macAddress ?? '—'}</p>
                                        <p><strong>Sistema Operacional:</strong> {selectedFinding.asset?.operatingSystem ?? '—'}</p>
                                        <p><strong>CPE:</strong> {selectedFinding.asset?.cpe ?? '—'}</p>
                                        <p><strong>Informações extras:</strong> {selectedFinding.asset?.extraInfo ?? '—'}</p>
                                    </section>
                                </div>
                            </div>  
                        </>
                    )}
                </aside>
            </section>
        </main>
    );
}