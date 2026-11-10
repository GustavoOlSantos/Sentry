import { useState } from "react";

import Modal from "../../components/modal";
import userService from "../../services/userService";
import { getApiErrorMessage } from "../../services/apiError";
import { ALL_ROLES, ROLE_DESCRIPTIONS, ROLE_LABELS, ROLES } from "../auth/roles";
import { isValidEmail, validatePassword } from "../auth/format";

/**
 * Criação e edição de usuário.
 *  state = { mode: "create" }            → novo usuário (pede senha)
 *  state = { mode: "edit", user }        → edição (nome, e-mail, perfil e status; a senha tem modal próprio)
 *  state = null                          → fechado
 */
export default function UserFormModal({ state, currentUserId, onClose, onSaved }) {
    const isEdit = state?.mode === "edit";

    return (
        <Modal
            isOpen={!!state}
            onClose={onClose}
            title={isEdit ? "Editar usuário" : "Novo usuário"}
        >
            {/* o formulário só existe com o modal aberto, então o estado reinicia a cada abertura */}
            {state && (
                <UserForm
                    key={state.user?.id ?? "novo"}
                    user={state.user}
                    isEdit={isEdit}
                    isSelf={isEdit && state.user.id === currentUserId}
                    onClose={onClose}
                    onSaved={onSaved}
                />
            )}
        </Modal>
    );
}

function UserForm({ user, isEdit, isSelf, onClose, onSaved }) {
    const [name, setName] = useState(user?.name ?? "");
    const [email, setEmail] = useState(user?.email ?? "");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [role, setRole] = useState(user?.role ?? ROLES.USER);
    const [active, setActive] = useState(user?.active ?? true);

    const [showPassword, setShowPassword] = useState(false);
    const [errors, setErrors] = useState({});
    const [serverError, setServerError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    function validate() {
        const found = {};

        if (!name.trim()) found.name = "Informe o nome.";
        else if (name.trim().length > 120) found.name = "O nome deve ter no máximo 120 caracteres.";

        if (!email.trim()) found.email = "Informe o e-mail.";
        else if (!isValidEmail(email.trim())) found.email = "E-mail inválido.";
        else if (email.trim().length > 180) found.email = "O e-mail deve ter no máximo 180 caracteres.";

        if (!isEdit) {
            const passwordError = validatePassword(password);
            if (passwordError) found.password = passwordError;
            else if (password !== confirmPassword) found.confirmPassword = "As senhas não conferem.";
        }

        return found;
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setServerError("");

        const found = validate();
        setErrors(found);
        if (Object.keys(found).length > 0) return;

        setSubmitting(true);

        try {
            const saved = isEdit
                ? await userService.update(user.id, { name: name.trim(), email: email.trim(), role, active })
                : await userService.create({ name: name.trim(), email: email.trim(), password, role });

            onSaved(saved, isEdit ? "Usuário atualizado com sucesso." : "Usuário criado com sucesso.");
        } catch (err) {
            setServerError(getApiErrorMessage(err, "Não foi possível salvar o usuário."));
            setSubmitting(false);
        }
    }

    return (
        <form className="users-form" onSubmit={handleSubmit} noValidate>
            {serverError && (
                <p className="auth-alert auth-alert--error" role="alert">
                    <i className="fa-solid fa-circle-exclamation" />
                    {serverError}
                </p>
            )}

            <label className="auth-field" htmlFor="user-name">
                <span>Nome</span>
                <input id="user-name" type="text" autoFocus maxLength={120} value={name}
                    onChange={(e) => setName(e.target.value)} disabled={submitting} />
                {errors.name && <small className="auth-field__error">{errors.name}</small>}
            </label>

            <label className="auth-field" htmlFor="user-email">
                <span>E-mail (usado para entrar)</span>
                <input id="user-email" type="email" autoComplete="off" maxLength={180} value={email}
                    onChange={(e) => setEmail(e.target.value)} disabled={submitting} />
                {errors.email && <small className="auth-field__error">{errors.email}</small>}
            </label>

            {!isEdit && (
                <>
                    <label className="auth-field" htmlFor="user-password">
                        <span>Senha inicial</span>

                        <div className="auth-password">
                            <input id="user-password" type={showPassword ? "text" : "password"} autoComplete="new-password"
                                value={password} onChange={(e) => setPassword(e.target.value)} disabled={submitting} />

                            <button type="button" className="auth-password-toggle" onClick={() => setShowPassword((v) => !v)}
                                aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}>
                                <i className={`fa-solid ${showPassword ? "fa-eye-slash" : "fa-eye"}`} />
                            </button>
                        </div>

                        {errors.password
                            ? <small className="auth-field__error">{errors.password}</small>
                            : <small className="auth-field__hint">Mínimo de 8 caracteres. O usuário pode trocá-la em "Meu perfil".</small>}
                    </label>

                    <label className="auth-field" htmlFor="user-confirm-password">
                        <span>Confirmar senha</span>
                        <input id="user-confirm-password" type={showPassword ? "text" : "password"} autoComplete="new-password"
                            value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} disabled={submitting} />
                        {errors.confirmPassword && <small className="auth-field__error">{errors.confirmPassword}</small>}
                    </label>
                </>
            )}

            <label className="auth-field" htmlFor="user-role">
                <span>Perfil de acesso</span>

                <select id="user-role" value={role} onChange={(e) => setRole(e.target.value)} disabled={submitting || isSelf}>
                    {ALL_ROLES.map((value) => (
                        <option key={value} value={value}>{ROLE_LABELS[value]}</option>
                    ))}
                </select>

                <small className="auth-field__hint">
                    {isSelf ? "Você não pode alterar o seu próprio perfil de acesso." : ROLE_DESCRIPTIONS[role]}
                </small>
            </label>

            {isEdit && (
                <label className="users-switch" htmlFor="user-active">
                    <input id="user-active" type="checkbox" checked={active}
                        onChange={(e) => setActive(e.target.checked)} disabled={submitting || isSelf} />

                    <span className="users-switch__track" aria-hidden="true" />

                    <span>
                        Conta ativa
                        <small className="auth-field__hint">
                            {isSelf
                                ? "Você não pode desativar a sua própria conta."
                                : "Usuários inativos não conseguem entrar e perdem o acesso imediatamente."}
                        </small>
                    </span>
                </label>
            )}

            <div className="users-form__actions">
                <button type="button" className="users-btn users-btn--ghost" onClick={onClose} disabled={submitting}>
                    Cancelar
                </button>

                <button type="submit" className="users-btn users-btn--primary" disabled={submitting}>
                    {submitting ? "Salvando..." : isEdit ? "Salvar alterações" : "Criar usuário"}
                </button>
            </div>
        </form>
    );
}
