import { useEffect, useState } from "react";
import policyService from "../../services/policyService";

const EMPTY_RULE = {
    name: "",
    description: "",
    type: "PORT_RESTRICTION",
    condition: "",
    expectedValue: "",
    severity: "MEDIUM",
    enabled: true,
};

const TYPE_LABELS = {
    PORT_RESTRICTION: "Restrição de porta",
    FORBIDDEN_SERVICE: "Serviço proibido",
};

function getApiErrorMessage(error, fallback) {
    return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
}

export default function RuleFormModal({ policy, rule, onClose, onSaved }) {
    const editing = Boolean(rule);
    const [form, setForm] = useState(EMPTY_RULE);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        setForm(rule ? {
            name: rule.name || "",
            description: rule.description || "",
            type: rule.type || "PORT_RESTRICTION",
            condition: rule.condition || "",
            expectedValue: rule.expectedValue || "",
            severity: rule.severity || "MEDIUM",
            enabled: rule.enabled !== false,
        } : EMPTY_RULE);
        setError("");
    }, [rule]);

    if (!policy) return null;

    const handleChange = (event) => {
        const { name, value, type, checked } = event.target;
        setForm((current) => ({
            ...current,
            [name]: type === "checkbox" ? checked : value,
        }));
    };

    const expectedLabel = form.type === "PORT_RESTRICTION"
        ? "Porta proibida"
        : "Serviço/produto proibido";

    const expectedPlaceholder = form.type === "PORT_RESTRICTION"
        ? "Ex.: 23"
        : "Ex.: telnet";

    const conditionHint = form.type === "PORT_RESTRICTION"
        ? "Opcional. Informe tcp ou udp para restringir a regra a um protocolo."
        : "Não utilizada para este tipo de regra.";

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!form.name.trim()) {
            setError("Informe o nome da regra.");
            return;
        }

        if (!form.expectedValue.trim()) {
            setError(`Informe ${form.type === "PORT_RESTRICTION" ? "a porta" : "o serviço/produto"} que será avaliado.`);
            return;
        }

        if (form.type === "PORT_RESTRICTION") {
            const port = Number(form.expectedValue.trim());
            if (!Number.isInteger(port) || port < 1 || port > 65535) {
                setError("A porta deve ser um número inteiro entre 1 e 65535.");
                return;
            }
        }

        setSaving(true);

        try {
            const payload = {
                policy: { id: policy.id },
                name: form.name.trim(),
                description: form.description.trim(),
                type: form.type,
                condition: form.type === "PORT_RESTRICTION" ? form.condition.trim() : "",
                expectedValue: form.expectedValue.trim(),
                severity: form.severity,
                enabled: form.enabled,
            };

            const saved = editing
                ? await policyService.updateRule(rule.id, payload)
                : await policyService.createRule(payload);

            onSaved(saved, editing ? "Regra atualizada." : "Regra criada.");
        } catch (err) {
            console.error("Falha ao salvar regra", err);
            setError(getApiErrorMessage(err, "Não foi possível salvar a regra."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="policy-modal-backdrop" onClick={onClose}>
            <div className="policy-modal policy-modal--wide" onClick={(event) => event.stopPropagation()}>
                <header className="policy-modal__header">
                    <div>
                        <span className="policy-modal__eyebrow">
                            <i className="fa-solid fa-list-check" />
                            {policy.name}
                        </span>
                        <h3>{editing ? "Editar regra" : "Nova regra"}</h3>
                    </div>
                    <button type="button" onClick={onClose} aria-label="Fechar">&times;</button>
                </header>

                <form className="policy-form" onSubmit={handleSubmit}>
                    <div className="policy-form__grid">
                        <div className="policy-form__field">
                            <label htmlFor="rule-name">Nome</label>
                            <input
                                id="rule-name"
                                name="name"
                                value={form.name}
                                onChange={handleChange}
                                placeholder="Ex.: Bloquear Telnet"
                                maxLength={120}
                                autoFocus
                            />
                        </div>

                        <div className="policy-form__field">
                            <label htmlFor="rule-type">Tipo</label>
                            <select id="rule-type" name="type" value={form.type} onChange={handleChange}>
                                {Object.entries(TYPE_LABELS).map(([value, label]) => (
                                    <option key={value} value={value}>{label}</option>
                                ))}
                            </select>
                        </div>

                        <div className="policy-form__field">
                            <label htmlFor="rule-expected">{expectedLabel}</label>
                            <input
                                id="rule-expected"
                                name="expectedValue"
                                value={form.expectedValue}
                                onChange={handleChange}
                                placeholder={expectedPlaceholder}
                                inputMode={form.type === "PORT_RESTRICTION" ? "numeric" : "text"}
                            />
                        </div>

                        <div className="policy-form__field">
                            <label htmlFor="rule-condition">Condição</label>
                            <input
                                id="rule-condition"
                                name="condition"
                                value={form.condition}
                                onChange={handleChange}
                                disabled={form.type !== "PORT_RESTRICTION"}
                                placeholder={form.type === "PORT_RESTRICTION" ? "tcp, udp ou vazio" : "Não aplicável"}
                            />
                            <small>{conditionHint}</small>
                        </div>

                        <div className="policy-form__field">
                            <label htmlFor="rule-severity">Severidade</label>
                            <select id="rule-severity" name="severity" value={form.severity} onChange={handleChange}>
                                <option value="LOW">Baixa</option>
                                <option value="MEDIUM">Média</option>
                                <option value="HIGH">Alta</option>
                                <option value="CRITICAL">Crítica</option>
                            </select>
                        </div>

                        <label className="policy-switch">
                            <input
                                type="checkbox"
                                name="enabled"
                                checked={form.enabled}
                                onChange={handleChange}
                            />
                            <span className="policy-switch__track" />
                            <span>
                                <strong>Regra habilitada</strong>
                                <small>Regras desabilitadas ficam cadastradas, mas não são avaliadas.</small>
                            </span>
                        </label>
                    </div>

                    <div className="policy-form__field">
                        <label htmlFor="rule-description">Descrição</label>
                        <textarea
                            id="rule-description"
                            name="description"
                            value={form.description}
                            onChange={handleChange}
                            placeholder="Explique o que esta regra pretende impedir ou controlar."
                            rows={4}
                        />
                    </div>

                    <div className="policy-rule-help">
                        <i className="fa-solid fa-circle-info" />
                        <span>
                            {form.type === "PORT_RESTRICTION"
                                ? "A regra será violada quando uma porta aberta corresponder ao valor informado e, se preenchido, ao protocolo."
                                : "A regra será violada quando o nome do serviço ou produto identificado pelo Nmap contiver o valor informado."}
                        </span>
                    </div>

                    {error && <p className="policy-form__error"><i className="fa-solid fa-circle-exclamation" />{error}</p>}

                    <footer className="policy-form__actions">
                        <button type="button" className="policies-btn policies-btn--ghost" onClick={onClose} disabled={saving}>
                            Cancelar
                        </button>
                        <button type="submit" className="policies-btn policies-btn--primary" disabled={saving}>
                            <i className={`fa-solid ${saving ? "fa-spinner fa-spin" : "fa-floppy-disk"}`} />
                            {saving ? "Salvando..." : editing ? "Salvar alterações" : "Criar regra"}
                        </button>
                    </footer>
                </form>
            </div>
        </div>
    );
}
