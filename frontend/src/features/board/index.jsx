
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Select from 'react-select';

import api from '../../services/api';
import userService from '../../services/userService';
import { getInitials } from '../auth/format';

import { SideBar } from "../../components/sidebar";
import '../../styles/board.css';
import '../../styles/user-menu.css';

import dev_gustavo from '../../assets/devs/gustavo.jpg';
import dev_leandro from '../../assets/devs/leandro.jpg';
import dev_sophia from '../../assets/devs/sophia.jpg';
import dev_vivian from '../../assets/devs/vivian.jpg';

const FINDING_COLUMNS = [
    { id: 'open', title: 'ABERTO', statuses: ['OPEN'], dropStatus: 'OPEN', userDroppable: true },
    { id: 'progress', title: 'EM ANDAMENTO', statuses: ['IN_PROGRESS', 'FAILED_VALIDATION'], dropStatus: 'IN_PROGRESS', userDroppable: true },
    { id: 'resolved', title: 'RESOLVIDA', statuses: ['RESOLVED'], dropStatus: 'RESOLVED', userDroppable: true },
    { id: 'closed', title: 'FECHADA', statuses: ['JUSTIFIED', 'VALIDATED'], dropStatus: 'VALIDATED', userDroppable: false }
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

const MANUAL_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'JUSTIFIED'];

const formatDate = (value) =>
    value ? new Date(value).toLocaleString('pt-BR') : '—';

// ---------- Formulário de gerenciamento (drawer) ----------
function FindingManageForm({ finding, developers, onChangeStatus, onChangeAssignee }) {

    const [status, setStatus] = useState(finding.status);
    const [justification, setJustification] = useState(finding.justification ?? '');
    const [submitting, setSubmitting] = useState(false);
    const [assigneeError, setAssigneeError] = useState(null);
    const [formError, setFormError] = useState(null);

    const needsJustification = status === 'JUSTIFIED';

    const dirty =
        status !== finding.status ||
        (needsJustification &&
            justification.trim() !== (finding.justification ?? '').trim());

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

    const handleAssigneeChange = async (selected) => {
        setAssigneeError(null);

        const success = await onChangeAssignee(
            finding.id,
            selected?.value ?? null
        );

        if (!success) {
            setAssigneeError('Não foi possível atualizar o responsável. A alteração foi desfeita.');
        }
    };

    return (
        <section className="drawer-section drawer-manage">
            <h5>Gerenciar</h5>

            <label className="manage-field">
                <span>Responsável</span>

                <Select
                    className="assignee-select"
                    classNamePrefix="assignee"
                    value={
                        developers
                            .map(user => ({
                                value: user.id,
                                label: user.name
                            }))
                            .find(option => option.value === finding.assigneeId) || null
                    }
                    options={developers.map(user => ({
                        value: user.id,
                        label: user.name
                    }))}
                    onChange={handleAssigneeChange}
                    isClearable
                    formatOptionLabel={(option) => (
                        <div className="assignee-option users-identity">
                            <span className="user-avatar">
                                {getInitials(option.label)}
                            </span>

                            <span>{option.label}</span>
                        </div>
                    )}
                />

                {assigneeError && (
                    <p className="manage-error">{assigneeError}</p>
                )}
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
                        {!MANUAL_STATUSES.includes(finding.status) && (
                            <option value={finding.status} disabled>
                                {STATUS_LABELS[finding.status] || finding.status}
                            </option>
                        )}

                        {MANUAL_STATUSES.map(s => (
                            <option key={s} value={s}>
                                {STATUS_LABELS[s]}
                            </option>
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

                {formError && (
                    <p className="manage-error">{formError}</p>
                )}

                <button
                    type="submit"
                    className="btn-update-drawer"
                    disabled={!dirty || submitting}
                >
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

    const [developers, setDevelopers] = useState([]);

    const assigneeOptions = useMemo(() => [
        { value: '', label: 'Responsáveis: Todos' },
        ...developers.map(user => ({
            value: user.id,
            label: user.name
        }))
    ], [developers]);

    // ---------- Filtros ----------
    const [findingSearchTerm, setFindingSearchTerm] = useState('');
    const [findingAssigneeFilter, setFindingAssigneeFilter] = useState('');
    const [findingStatusFilter, setFindingStatusFilter] = useState('');

    // ---------- Drag and drop / Drawer ----------
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

    // ---------- Busca dos findings ----------
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
            setFindings([]);
        } finally {
            setLoading(false);
        }
    }, []);

    const fetchDevelopers = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);

            const response = await userService.list();

            const data = Array.isArray(response)
                ? response
                : response?.content ?? [];

            const devs = data.filter(user => user.role === 'DEV');
            setDevelopers(devs);

        } catch (err) {
            console.error('Falha ao buscar os desenvolvedores:', err);
            setError('Não foi possível carregar os desenvolvedores.');
            setDevelopers([]);
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchDevelopers();
        fetchFindings();
    }, [fetchFindings, fetchDevelopers]);

    // ---------- Filtragem dos findings ----------
    const filteredFindings = useMemo(() => {
        const search = findingSearchTerm.trim().toLowerCase();

        return (findings ?? []).filter((finding) => {

            const matchesSearch =
                (finding.code ?? '').toLowerCase().includes(search) ||
                (finding.description ?? '').toLowerCase().includes(search) ||
                (finding.asset?.ipAddress ?? '').toLowerCase().includes(search) ||
                (finding.asset?.hostname ?? '').toLowerCase().includes(search) ||
                (finding.vulnerability?.cveId ?? '').toLowerCase().includes(search);

            const matchesAssignee = findingAssigneeFilter === '' || finding.assigneeId === findingAssigneeFilter;

            const matchesStatus = findingStatusFilter === '' || finding.status === findingStatusFilter;

            return matchesSearch && matchesAssignee && matchesStatus;
        });

    }, [findings, findingSearchTerm, findingAssigneeFilter, findingStatusFilter]);

    // ---------- Agrupamento por status ----------
    const groupedFindings = useMemo(() => {
        return FINDING_COLUMNS.map(column => ({
            ...column,
            findings: filteredFindings.filter(f =>
                column.statuses.includes(f.status)
            )
        }));
    }, [filteredFindings]);

    // ---------- Finding selecionado ----------
    const selectedFinding = useMemo(
        () => findings.find(f => f.id === selectedId) ?? null,
        [findings, selectedId]
    );

    // ---------- Atualização local ----------
    const updateFindingLocal = (id, patch) => {
        setFindings(prev =>
            prev.map(f => f.id === id ? { ...f, ...patch } : f)
        );
    };

    // ---------- Atualização de status ----------
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

    // ---------- Atualização de responsável ----------
    const handleAssigneeChange = async (id, assigneeId) => {

        const previous = findings.find(f => f.id === id)?.assigneeId;

        updateFindingLocal(id, { assigneeId });

        try {
            await api.patch(`/findings/${id}/assignee`, { assigneeId });

            return true;

        } catch (err) {
            console.error('Falha ao atualizar o responsável:', err);

            updateFindingLocal(id, { assigneeId: previous });

            return false;
        }
    };

    // ---------- Drag and drop ----------
    const handleDragStart = (event, finding) => {
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', String(finding.id));

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

        if (dragOverColumn !== column.id) {
            setDragOverColumn(column.id);
        }
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

        const onKeyDown = (e) =>
            e.key === 'Escape' && closeDrawer();

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

                    <button
                        className="btn-update"
                        onClick={fetchFindings}
                        disabled={loading}
                        type="button"
                    >
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

                        <button
                            type="button"
                            onClick={() => setWarning(null)}
                            aria-label="Fechar aviso"
                        >
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
                    <>

                        {/* ---------- Filtros ---------- */}
                        <section className="filter-table">

                            <div className="filter-search">
                                <i className="fa-solid fa-magnifying-glass"></i>

                                <input
                                    id="finding-search"
                                    name="finding-search"
                                    type="text"
                                    placeholder="Buscar por CVE, IP ou hostname..."
                                    value={findingSearchTerm}
                                    onChange={(e) => setFindingSearchTerm(e.target.value)}
                                />
                            </div>

                            <div className="filter-select">
                                <select id="status" name="status" value={findingStatusFilter} onChange={(e) => setFindingStatusFilter(e.target.value)} >
                                    <option value="">Status: Todos</option>

                                    {Object.entries(STATUS_LABELS).map(([value, label]) => (
                                        <option key={value} value={value}>
                                            {label}
                                        </option>
                                    ))}
                                </select>

                                <i className="fa-solid fa-chevron-down" aria-hidden="true"></i>
                            </div>

                            <Select
                                className="filter-select"
                                classNamePrefix="filter"
                                value={assigneeOptions.find(
                                    option => option.value === findingAssigneeFilter
                                )}
                                options={assigneeOptions}
                                onChange={(selected) =>
                                    setFindingAssigneeFilter(selected?.value ?? '')
                                }
                                isClearable={false}
                                placeholder="Responsáveis: Todos"
                                formatOptionLabel={(option) => (
                                    <div className="assignee-option users-identity">

                                        {option.label == "Responsáveis: Todos" ? (
                                            null
                                        ) : (
                                            <span className="user-avatar">
                                                {getInitials(option.label)}
                                            </span>
                                        )}
                                        

                                        <span>{option.label}</span>
                                    </div>
                                )}
                            />

                        </section>

                        {/* ---------- Kanban ---------- */}
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
                                            <span className={`kanban-column-dot ${column.id}`}>
                                                •
                                            </span>

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

                                                    <h4>
                                                        {finding.title || 'Finding sem título'}
                                                        {' - '}
                                                        {finding.asset?.ipAddress}
                                                    </h4>

                                                    <p>
                                                        {finding.description || 'Finding sem descrição'}
                                                    </p>

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

                                                    <div className="finding-card-footer">

                                                        <p>#{finding.id.slice(0, 8)}</p>

                                                        {finding.assigneeId && (
                                                            <span
                                                                className="user-avatar"
                                                                title={
                                                                    developers.find(
                                                                        user => user.id === finding.assigneeId
                                                                    )?.name || finding.assigneeId
                                                                }
                                                            >
                                                                {getInitials(
                                                                    developers.find(
                                                                        user => user.id === finding.assigneeId
                                                                    )?.name || ''
                                                                )}
                                                            </span>
                                                        )}

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

                    </>
                )}

                {/* ---------- Drawer ---------- */}
                <div
                    className={`drawer-overlay ${selectedFinding ? 'open' : ''}`}
                    onClick={closeDrawer}
                />

                <aside
                    className={`finding-drawer ${selectedFinding ? 'open' : ''}`}
                    aria-hidden={!selectedFinding}
                >

                    {selectedFinding && (
                        <>

                            <header className="drawer-header">
                                <h3>
                                    {selectedFinding.title || 'Finding sem título'}
                                </h3>

                                <button
                                    type="button"
                                    className="drawer-close"
                                    onClick={closeDrawer}
                                    aria-label="Fechar"
                                >
                                    <i className="fa-solid fa-xmark"></i>
                                </button>
                            </header>

                            <div className="drawer-manage-container">

                                <aside>
                                    <FindingManageForm
                                        key={selectedFinding.id}
                                        finding={selectedFinding}
                                        developers={developers}
                                        onChangeStatus={changeStatus}
                                        onChangeAssignee={handleAssigneeChange}
                                    />
                                </aside>

                                <div className="drawer-body">

                                    <section className="drawer-section">

                                        <h5>Descrição: {selectedFinding.code}</h5>

                                        <p>
                                            {selectedFinding.description || 'Sem descrição.'}
                                        </p>

                                        <br></br>

                                        <p>
                                            {selectedFinding.evidence || 'Sem descrição.'}
                                        </p>

                                        <div className="drawer-dates">
                                            <h5>Criado em:</h5>
                                            <p>{formatDate(selectedFinding.detectedAt)}</p>
                                        </div>

                                        <div className="drawer-dates">
                                            <h5>Resolvido em:</h5>
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

                                        <p>
                                            <strong>Hostname:</strong>{' '}
                                            {selectedFinding.asset?.hostname ?? '—'}
                                        </p>

                                        <p>
                                            <strong>MAC:</strong>{' '}
                                            {selectedFinding.asset?.macAddress ?? '—'}
                                        </p>

                                        <p>
                                            <strong>Sistema Operacional:</strong>{' '}
                                            {selectedFinding.asset?.operatingSystem ?? '—'}
                                        </p>

                                        <p>
                                            <strong>CPE:</strong>{' '}
                                            {selectedFinding.service?.cpe ?? '—'}
                                        </p>

                                        <p>
                                            <strong>CVE:</strong>{' '}
                                            {selectedFinding?.code ?? '—'}
                                        </p>

                                        <p>
                                            <strong>Informações extras:</strong>{' '}
                                            {selectedFinding.service?.extraInfo ?? '—'}
                                        </p>

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
