import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Select, { defaultTheme } from 'react-select';

import { getInitials } from '../auth/format';

import '../../styles/board.css';
import '../../styles/user-menu.css';

const MANUAL_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'JUSTIFIED'];

const STATUS_LABELS = {
    OPEN: 'Aberto',
    IN_PROGRESS: 'Executando',
    FAILED_VALIDATION: 'Inválida',
    RESOLVED: 'Resolvida',
    JUSTIFIED: 'Justificada',
    VALIDATED: 'Validada'
};

// ---------- Formulário de gerenciamento (drawer) ----------
export function FindingManageForm({ finding, developers, onChangeStatus, onChangeAssignee }) {

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

export default FindingManageForm;