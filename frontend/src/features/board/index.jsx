import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../../services/api';
import { SideBar } from "../../components/sidebar";
import '../../styles/board.css';

const FINDING_COLUMNS = [
    { id: 'open',     title: 'ABERTO',       statuses: ['OPEN'],                              dropStatus: 'OPEN' },
    { id: 'progress', title: 'EM ANDAMENTO', statuses: ['IN_PROGRESS', 'FAILED_VALIDATION'],  dropStatus: 'IN_PROGRESS' },
    { id: 'resolved', title: 'RESOLVIDA',    statuses: ['RESOLVED'],                          dropStatus: 'RESOLVED' },
    { id: 'closed',   title: 'FECHADA',      statuses: ['JUSTIFIED', 'VALIDATED'],            dropStatus: 'VALIDATED' }
];

const STATUS_LABELS = {
    OPEN: 'Aberto',
    IN_PROGRESS: 'Em andamento',
    FAILED_VALIDATION: 'Falha na validação',
    RESOLVED: 'Resolvida',
    JUSTIFIED: 'Justificada',
    VALIDATED: 'Validada'
};

const TYPE_LABELS = {
    VULNERABILITY: 'Vulnerabilidade',
    POLICY_VIOLATION: 'Violação de política'
};

const formatDate = (value) =>
    value ? new Date(value).toLocaleString('pt-BR') : '—';

export default function Board() {
    const [findings, setFindings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const [draggedId, setDraggedId] = useState(null);
    const [dragOverColumn, setDragOverColumn] = useState(null);
    const [selectedId, setSelectedId] = useState(null);

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

    // ---------- Atualização de status ----------
    const updateStatusLocal = (id, status) =>
        setFindings(prev => prev.map(f => (f.id === id ? { ...f, status } : f)));

    const moveFinding = async (finding, column) => {
        if (column.statuses.includes(finding.status)) return; // mesma coluna

        const previousStatus = finding.status;
        const newStatus = column.dropStatus;

        updateStatusLocal(finding.id, newStatus); // optimistic
        setError(null);

        try {
            await api.post(`/findings/${finding.id}/status`, { status: newStatus });
        } catch (err) {
            console.error('Falha ao atualizar status:', err);
            updateStatusLocal(finding.id, previousStatus); // rollback
            setError('Não foi possível mover o finding. A alteração foi desfeita.');
        }
    };

    // ---------- Drag and drop ----------
    const handleDragStart = (event, finding) => {
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', String(finding.id)); // necessário no Firefox
        setDraggedId(finding.id);
    };

    const handleDragEnd = () => {
        setDraggedId(null);
        setDragOverColumn(null);
    };

    const handleDragOver = (event, columnId) => {
        event.preventDefault(); // obrigatório para permitir o drop
        event.dataTransfer.dropEffect = 'move';
        if (dragOverColumn !== columnId) setDragOverColumn(columnId);
    };

    const handleDragLeave = (event) => {
        // ignora quando o mouse só passa para um elemento filho
        if (!event.currentTarget.contains(event.relatedTarget)) {
            setDragOverColumn(null);
        }
    };

    const handleDrop = (event, column) => {
        event.preventDefault();
        const finding = findings.find(f => f.id === draggedId);
        handleDragEnd();
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

            {loading && findings.length === 0 ? (
                <div className="findings-empty">
                    <i className="fa-solid fa-spinner fa-spin"></i>
                    <p>Carregando findings...</p>
                </div>
            ) : (
                <section className="kanban-board">
                    {groupedFindings.map(column => (
                        <article
                            className={`kanban-column ${dragOverColumn === column.id ? 'drag-over' : ''}`}
                            key={column.id}
                            onDragOver={(e) => handleDragOver(e, column.id)}
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
                    </>
                )}
            </aside>
        </main>
    );
}