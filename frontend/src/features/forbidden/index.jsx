import { Link, useNavigate } from "react-router-dom";

import "../../styles/not-found.css";

import { useAuth } from "../auth/useAuth";
import { ROLE_LABELS } from "../auth/roles";

/** Exibida quando o usuário está logado, mas o perfil dele não permite abrir a página (HTTP 403). */
function Forbidden() {
  const navigate = useNavigate();
  const { user } = useAuth();

  return (
    <main className="not-found">
      <section className="not-found__content">
        <div className="not-found__illustration" aria-hidden="true">
          <div className="not-found__orbit not-found__orbit--outer" />
          <div className="not-found__orbit not-found__orbit--inner" />

          <div className="not-found__shield">
            <i className="fa-solid fa-lock" style={{ fontSize: "56px" }} />
          </div>

          <span className="not-found__code not-found__code--top">403</span>
          <span className="not-found__code not-found__code--bottom">FORBIDDEN</span>
          <span className="not-found__dot not-found__dot--one" />
          <span className="not-found__dot not-found__dot--two" />
        </div>

        <div className="not-found__message">
          <span className="not-found__eyebrow">
            <span className="not-found__status-dot" /> ERRO 403 · ACESSO NEGADO </span>

          <h1>Você não tem <br />permissão para <span>esta página.</span>
          </h1>

          <p>
            Seu perfil{user ? ` (${ROLE_LABELS[user.role] ?? user.role})` : ""} não permite acessar este recurso.
            Se acredita que isso é um engano, solicite a um administrador a revisão do seu acesso.
          </p>

          <div className="not-found__actions">
            <Link to="/" className="not-found__button not-found__button--primary">
              <i className="fa-solid fa-house" />
              Voltar ao painel
            </Link>

            <button type="button" className="not-found__button not-found__button--secondary" onClick={() => navigate(-1)}>
              <i className="fa-solid fa-arrow-left" />
              Página anterior
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}

export default Forbidden;
