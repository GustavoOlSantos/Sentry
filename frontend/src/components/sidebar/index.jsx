import { useState, useEffect } from "react";
import { NavLink, useLocation } from "react-router-dom";

import "../../styles/home.css";

import { useAuth } from "../../features/auth/useAuth";

export function SideBar() {
    const location = useLocation();
    const isDashboard = location.pathname === "/";
    const { can } = useAuth();

    const [collapsed, setCollapsed] = useState(() => {
        return localStorage.getItem("sidebar-collapsed") === "true";
    });

    useEffect(() => {
        if (isDashboard) {
            setCollapsed(false);
        } else {
            setCollapsed(
                localStorage.getItem("sidebar-collapsed") === "true"
            );
        }
    }, [isDashboard]);

    const toggleSidebar = () => {
        if (isDashboard) return;

        const nextValue = !collapsed;

        setCollapsed(nextValue);
        localStorage.setItem("sidebar-collapsed", String(nextValue));
    };

    return (
        
        <aside className={`home_sidebar section ${ collapsed ? "collapsed" : "" }`}>
            <button type="button" className="sidebar-toggle" onClick={toggleSidebar} disabled={isDashboard} aria-label="Alternar menu">
                <i className={`fas ${ collapsed ? "fa-chevron-right" : "fa-chevron-left"}`} />
            </button>

            <ul>
                <li>
                    <NavLink to="/" end>
                        <i className="fas fa-tachograph-digital"></i>

                        <span className="sidebar-text">
                            Dashboard
                            <p>
                                Visão geral dos ativos,
                                findings e varreduras.
                            </p>
                        </span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/board">
                        <i className="fa-brands fa-trello"></i>

                        <span className="sidebar-text">
                            Correções Pendentes
                            <p>
                                Cards para resolução dos
                                achados
                            </p>
                        </span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/scans">
                        <i className="fas fa-gauge-simple"></i>

                        <span className="sidebar-text">
                            Varreduras
                            <p>
                                Lista de solicitações,
                                progresso e histórico.
                            </p>
                        </span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/hosts">
                        <i className="fas fa-server"></i>

                        <span className="sidebar-text">
                            Ativos
                            <p>
                                Inventário de hosts,
                                serviços e histórico.
                            </p>
                        </span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/reports">
                        <i className="fas fa-file-shield"></i>

                        <span className="sidebar-text">
                            Relatórios
                            <p>
                                Resultados e histórico do
                                ambiente.
                            </p>
                        </span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/policies">
                        <i className="fas fa-shield-halved"></i>

                        <span className="sidebar-text">
                            Políticas
                            <p>
                                Regras de segurança e
                                critérios de conformidade.
                            </p>
                        </span>
                    </NavLink>
                </li>

                {can("manageUsers") && (
                    <li>
                        <NavLink to="/usuarios">
                            <i className="fas fa-users"></i>

                            <span className="sidebar-text">
                                Usuários
                                <p>
                                    Contas, perfis de acesso
                                    e permissões.
                                </p>
                            </span>
                        </NavLink>
                    </li>
                )}
            </ul>
        </aside>
    );
}