import { useEffect, useState } from "react";
import policyService from "../../services/policyService";

const EMPTY_POLICY = {
    name: "",
    description: "",
    status: "ACTIVE",
};

function getApiErrorMessage(error, fallback) {
    return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
}

export default function PolicyFormModal({ policy, onClose, onSaved }) {
    const editing = Boolean(policy);
    const [form, setForm] = useState(EMPTY_POLICY);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        setForm(policy ? {
            name: policy.name || "",
            description: policy.description || "",
            status: policy.status || "ACTIVE",
        } : EMPTY_POLICY);
        setError("");
    }, [policy]);

    if (policy === undefined) return null;

    const handleChange = (event) => {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!form.name.trim()) {
            setError("Informe o nome da política.");
            return;
        }

        setSaving(true);

        try {
            const payload = {
                name: form.name.trim(),
                description: form.description.trim(),
                status: form.status,
            };

            const saved = editing
                ? await policyService.update(policy.id, payload)
                : await policyService.create(payload);

            onSaved(saved, editing ? "Política atualizada." : "Política criada.");
        } catch (err) {
            console.error("Falha ao salvar política", err);
            setError(getApiErrorMessage(err, "Não foi possível salvar a política."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="policy-modal-backdrop" onClick={onClose}>
            <div className="policy-modal" onClick={(event) => event.stopPropagation()}>
                <header className="policy-modal__header">
                    <div>
                        <span className="policy-modal__eyebrow">
                            <i className="fa-solid fa-shield-halved" />
                            Configuração
                        </span>
                        <h3>{editing ? "Editar política" : "Nova política"}</h3>
                    </div>
                    <button type="button" onClick={onClose} aria-label="Fechar">&times;</button>
                </header>

                <form className="policy-form" onSubmit={handleSubmit}>
                    <div className="policy-form__field">
                        <label htmlFor="policy-name">Nome</label>
                        <input
                            id="policy-name"
                            name="name"
                            value={form.name}
                            onChange={handleChange}
                            placeholder="Ex.: Política de serviços proibidos"
                            maxLength={120}
                            autoFocus
                        />
                    </div>

                    <div className="policy-form__field">
                        <label htmlFor="policy-description">Descrição</label>
                        <textarea
                            id="policy-description"
                            name="description"
                            value={form.description}
                            onChange={handleChange}
                            placeholder="Descreva o objetivo e o escopo desta política."
                            rows={4}
                        />
                    </div>

                    <div className="policy-form__field">
                        <label htmlFor="policy-status">Status</label>
                        <select id="policy-status" name="status" value={form.status} onChange={handleChange}>
                            <option value="ACTIVE">Ativa</option>
                            <option value="INACTIVE">Inativa</option>
                        </select>
                        <small>Somente políticas ativas participam da avaliação automática durante os scans.</small>
                    </div>

                    {error && <p className="policy-form__error"><i className="fa-solid fa-circle-exclamation" />{error}</p>}

                    <footer className="policy-form__actions">
                        <button type="button" className="policies-btn policies-btn--ghost" onClick={onClose} disabled={saving}>
                            Cancelar
                        </button>
                        <button type="submit" className="policies-btn policies-btn--primary" disabled={saving}>
                            <i className={`fa-solid ${saving ? "fa-spinner fa-spin" : "fa-floppy-disk"}`} />
                            {saving ? "Salvando..." : editing ? "Salvar alterações" : "Criar política"}
                        </button>
                    </footer>
                </form>
            </div>
        </div>
    );
}
