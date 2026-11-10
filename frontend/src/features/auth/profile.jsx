import { useState } from "react";
import { useNavigate } from "react-router-dom";

import "../../styles/auth.css";
import "../../styles/users.css";

import { SideBar } from "../../components/sidebar";
import authService from "../../services/authService";
import { getApiErrorMessage } from "../../services/apiError";

import { useAuth } from "./useAuth";
import RoleBadge from "./RoleBadge";
import { ALL_ROLES, PERMISSIONS, ROLE_DESCRIPTIONS, ROLE_LABELS } from "./roles";
import { formatDate, formatDateTime, getInitials, validatePassword } from "./format";

const PERMISSION_LABELS = {
    runScan: "Executar varreduras",
    editFindings: "Alterar status e responsável dos findings",
    editAssets: "Editar ativos",
    deleteAssets: "Excluir ativos",
    writePolicies: "Criar e editar políticas",
    manageUsers: "Gerenciar usuários",
};

export function Profile() {
    const { user, logout, can } = useAuth();
    const navigate = useNavigate();

    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");

        if (!currentPassword) return setError("Informe a senha atual.");

        const passwordError = validatePassword(newPassword);
        if (passwordError) return setError(passwordError);
        if (newPassword !== confirmPassword) return setError("A confirmação não confere com a nova senha.");
        if (newPassword === currentPassword) return setError("A nova senha deve ser diferente da atual.");

        setSubmitting(true);

        try {
            await authService.changePassword(currentPassword, newPassword);

            // o servidor invalida os tokens anteriores à troca: o usuário precisa entrar de novo
            logout("Senha alterada com sucesso. Entre novamente com a nova senha.");
            navigate("/login", { replace: true });
        } catch (err) {
            setError(getApiErrorMessage(err, "Não foi possível alterar a senha."));
            setSubmitting(false);
        }
    }

    return (
        <main className="users-container">
            <SideBar />

            <section className="users-content">
                <div className="users-heading">
                    <div>
                        <h2>Meu perfil</h2>
                        <p>Seus dados de acesso e a troca de senha.</p>
                    </div>
                </div>

                <div className="profile-grid">
                    <section className="profile-card">
                        <div className="profile-identity">
                            <span className="user-avatar user-avatar--large">{getInitials(user.name)}</span>

                            <div>
                                <h3>{user.name}</h3>
                                <p>{user.email}</p>
                                <RoleBadge role={user.role} />
                            </div>
                        </div>

                        <dl className="profile-details">
                            <div>
                                <dt>Perfil de acesso</dt>
                                <dd>{ROLE_LABELS[user.role]} — {ROLE_DESCRIPTIONS[user.role]}</dd>
                            </div>
                            <div>
                                <dt>Membro desde</dt>
                                <dd>{formatDate(user.createdAt)}</dd>
                            </div>
                            <div>
                                <dt>Último acesso</dt>
                                <dd>{formatDateTime(user.lastLoginAt)}</dd>
                            </div>
                        </dl>

                        <h4 className="profile-subtitle">O que seu perfil pode fazer</h4>
                        <ul className="profile-permissions">
                            <li className="allowed"><i className="fa-solid fa-check" /> Visualizar ativos, findings, varreduras e relatórios</li>
                            {Object.keys(PERMISSIONS).map((permission) => (
                                <li key={permission} className={can(permission) ? "allowed" : "denied"}>
                                    <i className={`fa-solid ${can(permission) ? "fa-check" : "fa-xmark"}`} />
                                    {PERMISSION_LABELS[permission]}
                                </li>
                            ))}
                        </ul>

                        <p className="profile-roles-hint">
                            Perfis existentes: {ALL_ROLES.map((role) => ROLE_LABELS[role]).join(", ")}.
                        </p>
                    </section>

                    <section className="profile-card">
                        <h3>Alterar senha</h3>
                        <p className="profile-card__intro">Ao trocar a senha, todas as sessões abertas são encerradas e você entra de novo.</p>

                        {error && (
                            <p className="auth-alert auth-alert--error" role="alert">
                                <i className="fa-solid fa-circle-exclamation" />
                                {error}
                            </p>
                        )}

                        <form className="users-form" onSubmit={handleSubmit} noValidate>
                            <label className="auth-field" htmlFor="profile-current">
                                <span>Senha atual</span>
                                <input id="profile-current" type={showPassword ? "text" : "password"} autoComplete="current-password"
                                    value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} disabled={submitting} />
                            </label>

                            <label className="auth-field" htmlFor="profile-new">
                                <span>Nova senha</span>

                                <div className="auth-password">
                                    <input id="profile-new" type={showPassword ? "text" : "password"} autoComplete="new-password"
                                        value={newPassword} onChange={(e) => setNewPassword(e.target.value)} disabled={submitting} />

                                    <button type="button" className="auth-password-toggle" onClick={() => setShowPassword((v) => !v)}
                                        aria-label={showPassword ? "Ocultar senhas" : "Mostrar senhas"}>
                                        <i className={`fa-solid ${showPassword ? "fa-eye-slash" : "fa-eye"}`} />
                                    </button>
                                </div>

                                <small className="auth-field__hint">Mínimo de 8 caracteres.</small>
                            </label>

                            <label className="auth-field" htmlFor="profile-confirm">
                                <span>Confirmar nova senha</span>
                                <input id="profile-confirm" type={showPassword ? "text" : "password"} autoComplete="new-password"
                                    value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} disabled={submitting} />
                            </label>

                            <div className="users-form__actions">
                                <button type="submit" className="users-btn users-btn--primary" disabled={submitting}>
                                    {submitting ? "Salvando..." : "Alterar senha"}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            </section>
        </main>
    );
}

export default Profile;
