import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import "../../styles/auth.css";
import "../../styles/users.css";

import { SideBar } from "../../components/sidebar";
import userService from "../../services/userService";
import { getApiErrorMessage } from "../../services/apiError";

import { useAuth } from "../auth/useAuth";
import RoleBadge from "../auth/RoleBadge";
import { ALL_ROLES, ROLE_LABELS, ROLES } from "../auth/roles";
import { formatDate, formatDateTime, getInitials } from "../auth/format";

import UserFormModal from "./user-form-modal";
import PasswordModal from "./password-modal";
import DeleteUserModal from "./delete-user-modal";

export function Users() {
    const { user: me, refreshUser, logout } = useAuth();
    const navigate = useNavigate();

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [reloadKey, setReloadKey] = useState(0);

    const [search, setSearch] = useState("");
    const [roleFilter, setRoleFilter] = useState("todos");
    const [statusFilter, setStatusFilter] = useState("todos");

    const [formState, setFormState] = useState(null);
    const [passwordTarget, setPasswordTarget] = useState(null);
    const [deleteTarget, setDeleteTarget] = useState(null);
    const [notice, setNotice] = useState(null);

    useEffect(() => {
        let cancelled = false;

        userService.list()
            .then((data) => {
                if (cancelled) return;
                setUsers(data);
                setError(null);
            })
            .catch((err) => {
                if (cancelled) return;
                console.error("Falha ao obter a listagem de usuários", err);
                setError(getApiErrorMessage(err, "Não foi possível carregar os usuários."));
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [reloadKey]);

    const reload = () => {
        setLoading(true);
        setReloadKey((key) => key + 1);
    };

    const totals = useMemo(() => ({
        all: users.length,
        active: users.filter((u) => u.active).length,
        admins: users.filter((u) => u.role === ROLES.ADM).length,
    }), [users]);

    const filteredUsers = useMemo(() => {
        const term = search.trim().toLowerCase();

        return users.filter((u) => {
            const matchesSearch =
                u.name.toLowerCase().includes(term) || u.email.toLowerCase().includes(term);
            const matchesRole = roleFilter === "todos" || u.role === roleFilter;
            const matchesStatus =
                statusFilter === "todos" || (statusFilter === "ativos" ? u.active : !u.active);

            return matchesSearch && matchesRole && matchesStatus;
        });
    }, [users, search, roleFilter, statusFilter]);

    const showNotice = (message) => setNotice(message);

    const handleSaved = (saved, message) => {
        setFormState(null);
        showNotice(message);
        reload();

        // editou o próprio cadastro: atualiza nome/e-mail mostrados no menu
        if (saved.id === me.id) {
            refreshUser().catch(() => {});
        }
    };

    const handlePasswordDone = (target) => {
        setPasswordTarget(null);

        if (target.id === me.id) {
            // o servidor invalida os tokens da conta: entra de novo com a senha nova
            logout("Senha alterada. Entre novamente com a nova senha.");
            navigate("/login", { replace: true });
            return;
        }

        showNotice(`Senha de ${target.name} redefinida. As sessões abertas dele foram encerradas.`);
    };

    const handleDeleted = (target) => {
        setDeleteTarget(null);
        showNotice(`Usuário ${target.name} excluído.`);
        reload();
    };

    return (
        <main className="users-container">
            <SideBar />

            <section className="users-content">
                <div className="users-heading">
                    <div>
                        <h2>Usuários</h2>
                        <p>Contas de acesso ao Sentry e seus perfis de permissão.</p>
                    </div>

                    <button type="button" className="users-btn users-btn--primary" onClick={() => setFormState({ mode: "create" })}>
                        <i className="fa-solid fa-user-plus" />
                        Novo usuário
                    </button>
                </div>

                <section className="users-overview">
                    <div className="users-overview__card">
                        <i className="fa-solid fa-users" />
                        <div>
                            <h4>{totals.all}</h4>
                            <p>Usuários</p>
                        </div>
                    </div>

                    <div className="users-overview__card">
                        <i className="fa-solid fa-user-check" />
                        <div>
                            <h4>{totals.active}</h4>
                            <p>Ativos</p>
                        </div>
                    </div>

                    <div className="users-overview__card">
                        <i className="fa-solid fa-user-shield" />
                        <div>
                            <h4>{totals.admins}</h4>
                            <p>Administradores</p>
                        </div>
                    </div>
                </section>

                {notice && (
                    <p className="auth-alert auth-alert--success" role="status">
                        <i className="fa-solid fa-circle-check" />
                        {notice}
                        <button type="button" className="auth-alert__close" onClick={() => setNotice(null)} aria-label="Fechar aviso">&times;</button>
                    </p>
                )}

                <section className="users-filters">
                    <div className="users-filters__search">
                        <i className="fa-solid fa-magnifying-glass" />
                        <input type="text" placeholder="Buscar por nome ou e-mail" aria-label="Buscar usuários"
                            value={search} onChange={(e) => setSearch(e.target.value)} />
                    </div>

                    <div className="users-filters__select">
                        <select aria-label="Filtrar por perfil" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
                            <option value="todos">Perfil: Todos</option>
                            {ALL_ROLES.map((role) => (
                                <option key={role} value={role}>{ROLE_LABELS[role]}</option>
                            ))}
                        </select>
                        <i className="fa-solid fa-chevron-down" aria-hidden="true" />
                    </div>

                    <div className="users-filters__select">
                        <select aria-label="Filtrar por status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                            <option value="todos">Status: Todos</option>
                            <option value="ativos">Ativos</option>
                            <option value="inativos">Inativos</option>
                        </select>
                        <i className="fa-solid fa-chevron-down" aria-hidden="true" />
                    </div>

                    <button type="button" className="users-btn users-btn--ghost" onClick={reload} disabled={loading}>
                        <i className={`fa-solid fa-rotate ${loading ? "fa-spin" : ""}`} />
                        {loading ? "Atualizando..." : "Atualizar"}
                    </button>
                </section>

                <div className="users-table-wrapper">
                    <table className="users-table">
                        <thead>
                            <tr>
                                <th>Usuário</th>
                                <th>Perfil</th>
                                <th>Status</th>
                                <th>Último acesso</th>
                                <th>Criado em</th>
                                <th className="users-table__actions-col">Ações</th>
                            </tr>
                        </thead>

                        <tbody>
                            {filteredUsers.map((u) => {
                                const isSelf = u.id === me.id;

                                return (
                                    <tr key={u.id}>
                                        <td>
                                            <span className="users-identity">
                                                <span className="user-avatar">{getInitials(u.name)}</span>

                                                <span className="users-identity__text">
                                                    <strong>
                                                        {u.name}
                                                        {isSelf && <em className="users-self-tag">você</em>}
                                                    </strong>
                                                    <span>{u.email}</span>
                                                </span>
                                            </span>
                                        </td>

                                        <td><RoleBadge role={u.role} /></td>

                                        <td>
                                            <span className={`users-status ${u.active ? "users-status--active" : "users-status--inactive"}`}>
                                                {u.active ? "Ativo" : "Inativo"}
                                            </span>
                                        </td>

                                        <td>{formatDateTime(u.lastLoginAt) === "—" ? "Nunca entrou" : formatDateTime(u.lastLoginAt)}</td>

                                        <td>{formatDate(u.createdAt)}</td>

                                        <td className="users-table__actions-col">
                                            <div className="users-actions">
                                                <button type="button" className="users-icon-btn" title="Editar" aria-label={`Editar ${u.name}`}
                                                    onClick={() => setFormState({ mode: "edit", user: u })}>
                                                    <i className="fa-solid fa-pen" />
                                                </button>

                                                <button type="button" className="users-icon-btn" title="Redefinir senha" aria-label={`Redefinir a senha de ${u.name}`}
                                                    onClick={() => setPasswordTarget(u)}>
                                                    <i className="fa-solid fa-key" />
                                                </button>

                                                <button type="button" className="users-icon-btn users-icon-btn--danger"
                                                    title={isSelf ? "Você não pode excluir a si mesmo" : "Excluir"}
                                                    aria-label={`Excluir ${u.name}`}
                                                    disabled={isSelf}
                                                    onClick={() => setDeleteTarget(u)}>
                                                    <i className="fa-solid fa-trash-can" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>

                {error && <p className="auth-alert auth-alert--error" role="alert"><i className="fa-solid fa-circle-exclamation" />{error}</p>}

                {loading && users.length === 0 && <p className="users-message">Carregando usuários...</p>}

                {!loading && !error && filteredUsers.length === 0 && (
                    <p className="users-message">Nenhum usuário encontrado com os filtros atuais.</p>
                )}
            </section>

            <UserFormModal state={formState} currentUserId={me.id} onClose={() => setFormState(null)} onSaved={handleSaved} />
            <PasswordModal target={passwordTarget} onClose={() => setPasswordTarget(null)} onDone={handlePasswordDone} />
            <DeleteUserModal target={deleteTarget} onClose={() => setDeleteTarget(null)} onDone={handleDeleted} />
        </main>
    );
}

export default Users;
