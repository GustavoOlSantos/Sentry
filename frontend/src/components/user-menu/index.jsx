import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import "../../styles/user-menu.css";

import { useAuth } from "../../features/auth/useAuth";
import RoleBadge from "../../features/auth/RoleBadge";
import { getInitials } from "../../features/auth/format";

/** Menu do usuário logado (canto direito do header): perfil, gestão de usuários (ADM) e sair. */
function UserMenu() {
    const { user, logout, can } = useAuth();
    const navigate = useNavigate();

    const [open, setOpen] = useState(false);
    const containerRef = useRef(null);

    useEffect(() => {
        if (!open) return;

        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setOpen(false);
            }
        };
        const handleEscape = (event) => {
            if (event.key === "Escape") setOpen(false);
        };

        document.addEventListener("mousedown", handleClickOutside);
        document.addEventListener("keydown", handleEscape);

        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
            document.removeEventListener("keydown", handleEscape);
        };
    }, [open]);

    if (!user) return null;

    const handleLogout = () => {
        setOpen(false);
        logout();
        navigate("/login", { replace: true });
    };

    return (
        <div className="user-menu" ref={containerRef}>
            <button
                type="button"
                className="user-menu__trigger"
                onClick={() => setOpen((value) => !value)}
                aria-haspopup="menu"
                aria-expanded={open}
            >
                <span className="user-avatar">{getInitials(user.name)}</span>

                <span className="user-menu__name">{user.name}</span>

                <i className={`fa-solid fa-chevron-${open ? "up" : "down"} user-menu__chevron`} aria-hidden="true" />
            </button>

            {open && (
                <div className="user-menu__dropdown" role="menu">
                    <div className="user-menu__identity">
                        <strong>{user.name}</strong>
                        <span>{user.email}</span>
                        <RoleBadge role={user.role} />
                    </div>

                    <Link to="/perfil" role="menuitem" onClick={() => setOpen(false)}>
                        <i className="fa-solid fa-user" /> Meu perfil
                    </Link>

                    {can("manageUsers") && (
                        <Link to="/usuarios" role="menuitem" onClick={() => setOpen(false)}>
                            <i className="fa-solid fa-users" /> Usuários
                        </Link>
                    )}

                    <button type="button" role="menuitem" className="user-menu__logout" onClick={handleLogout}>
                        <i className="fa-solid fa-right-from-bracket" /> Sair
                    </button>
                </div>
            )}
        </div>
    );
}

export default UserMenu;
