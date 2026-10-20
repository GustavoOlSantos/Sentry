import { NavLink } from "react-router-dom";

import "../../styles/home.css";

export function SideBar() {
  return (
    <aside className="home_sidebar section">
      <ul>
        <li>
          <NavLink to="/" end>
            <i className="fas fa-tachograph-digital"></i>
            Dashboard
          </NavLink>
        </li>

        <li>
          <NavLink to="/cards">
            <i className="fa-brands fa-trello"></i>
            Correções Pendentes
          </NavLink>
        </li>

        <li>
          <NavLink to="/scans">
            <i className="fas fa-gauge-simple"></i>
            Varreduras
          </NavLink>
        </li>

        <li>
          <NavLink to="/reports">
            <i className="fas fa-file-shield"></i>
            Relatórios
          </NavLink>
        </li>

        <li>
          <NavLink to="/policies">
            <i className="fas fa-shield-halved"></i>
            Políticas
          </NavLink>
        </li>
      </ul>
    </aside>
  );
}