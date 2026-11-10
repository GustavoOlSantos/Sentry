import { useState } from "react";

import Modal from "../../components/modal";
import userService from "../../services/userService";
import { getApiErrorMessage } from "../../services/apiError";
import { validatePassword } from "../auth/format";

/** Definição de nova senha por um administrador (não pede a senha atual). */
export default function PasswordModal({ target, onClose, onDone }) {
    return (
        <Modal isOpen={!!target} onClose={onClose} title="Redefinir senha">
            {target && <PasswordForm key={target.id} target={target} onClose={onClose} onDone={onDone} />}
        </Modal>
    );
}

function PasswordForm({ target, onClose, onDone }) {
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");

        const passwordError = validatePassword(newPassword);
        if (passwordError) return setError(passwordError);
        if (newPassword !== confirmPassword) return setError("As senhas não conferem.");

        setSubmitting(true);

        try {
            await userService.resetPassword(target.id, newPassword);
            onDone(target);
        } catch (err) {
            setError(getApiErrorMessage(err, "Não foi possível redefinir a senha."));
            setSubmitting(false);
        }
    }

    return (
        <form className="users-form" onSubmit={handleSubmit} noValidate>
            <p className="users-form__intro">
                Defina uma nova senha para <strong>{target.name}</strong> ({target.email}).
                As sessões abertas desse usuário serão encerradas.
            </p>

            {error && (
                <p className="auth-alert auth-alert--error" role="alert">
                    <i className="fa-solid fa-circle-exclamation" />
                    {error}
                </p>
            )}

            <label className="auth-field" htmlFor="reset-password">
                <span>Nova senha</span>

                <div className="auth-password">
                    <input id="reset-password" type={showPassword ? "text" : "password"} autoComplete="new-password" autoFocus
                        value={newPassword} onChange={(e) => setNewPassword(e.target.value)} disabled={submitting} />

                    <button type="button" className="auth-password-toggle" onClick={() => setShowPassword((v) => !v)}
                        aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}>
                        <i className={`fa-solid ${showPassword ? "fa-eye-slash" : "fa-eye"}`} />
                    </button>
                </div>

                <small className="auth-field__hint">Mínimo de 8 caracteres.</small>
            </label>

            <label className="auth-field" htmlFor="reset-confirm">
                <span>Confirmar nova senha</span>
                <input id="reset-confirm" type={showPassword ? "text" : "password"} autoComplete="new-password"
                    value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} disabled={submitting} />
            </label>

            <div className="users-form__actions">
                <button type="button" className="users-btn users-btn--ghost" onClick={onClose} disabled={submitting}>
                    Cancelar
                </button>

                <button type="submit" className="users-btn users-btn--primary" disabled={submitting}>
                    {submitting ? "Salvando..." : "Redefinir senha"}
                </button>
            </div>
        </form>
    );
}
