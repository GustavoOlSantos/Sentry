import { useState } from "react";

import Modal from "../../components/modal";
import userService from "../../services/userService";
import { getApiErrorMessage } from "../../services/apiError";

/** Confirmação de exclusão. O backend recusa excluir o único administrador ativo. */
export default function DeleteUserModal({ target, onClose, onDone }) {
    return (
        <Modal isOpen={!!target} onClose={onClose} title="Excluir usuário">
            {target && <DeleteForm key={target.id} target={target} onClose={onClose} onDone={onDone} />}
        </Modal>
    );
}

function DeleteForm({ target, onClose, onDone }) {
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleDelete() {
        setError("");
        setSubmitting(true);

        try {
            await userService.remove(target.id);
            onDone(target);
        } catch (err) {
            setError(getApiErrorMessage(err, "Não foi possível excluir o usuário."));
            setSubmitting(false);
        }
    }

    return (
        <div className="users-form">
            <p className="users-form__intro">
                Tem certeza de que deseja excluir <strong>{target.name}</strong> ({target.email})?
                Esta ação não pode ser desfeita. Se o objetivo é apenas bloquear o acesso,
                prefira desativar a conta na edição do usuário.
            </p>

            {error && (
                <p className="auth-alert auth-alert--error" role="alert">
                    <i className="fa-solid fa-circle-exclamation" />
                    {error}
                </p>
            )}

            <div className="users-form__actions">
                <button type="button" className="users-btn users-btn--ghost" onClick={onClose} disabled={submitting}>
                    Cancelar
                </button>

                <button type="button" className="users-btn users-btn--danger" onClick={handleDelete} disabled={submitting}>
                    {submitting ? "Excluindo..." : "Excluir usuário"}
                </button>
            </div>
        </div>
    );
}
