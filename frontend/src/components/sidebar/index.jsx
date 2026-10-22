import { NavLink } from "react-router-dom";

import "../../styles/home.css";

export function SideBar() {
  return (
    <aside className="home_sidebar section">
      <ul>
        <li>
          <NavLink to="/" end>
            <i className="fas fa-tachograph-digital"></i>
           <span>
                Dashboard
                <p>Visão geral dos ativos, findings e varreduras.</p>
            </span>
          </NavLink>
        </li>

        <li>
          <NavLink to="/cards">
            <i className="fa-brands fa-trello"></i>
            <span>
                Correções Pendentes
                <p>Cards para resolução dos achados</p>
            </span>
          </NavLink>
        </li>

        <li>
          <NavLink to="/scans">
            <i className="fas fa-gauge-simple"></i>
            <span>
                Varreduras
                <p>Lista de solicitações, progresso e histórico.</p>
            </span>
          </NavLink>
        </li>

        <li>
          <NavLink to="/hosts">
            <i className="fas fa-server"></i>
            <span>
                Ativos
                <p>Inventário de hosts, serviços e histórico.</p>
            </span>
          </NavLink>
        </li>

        <li>
          <NavLink to="/reports">
            <i className="fas fa-file-shield"></i>
            <span>
              Relatórios
              <p>Resultados e histórico do ambiente.</p>
            </span>
          </NavLink>
        </li>

        <li>
          <NavLink to="/policies">
            <i className="fas fa-shield-halved"></i>
            <span>
              Políticas
              <p>Regras de segurança e critérios de conformidade.</p>
            </span>
          </NavLink>
        </li>
      </ul>
    </aside>
  );
}