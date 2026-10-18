
import { Link, useNavigate } from 'react-router-dom';
import '../../styles/not-found.css';

function NotFound() {
  const navigate = useNavigate();

  return (
    <main className="not-found">
      <section className="not-found__content">
        <div className="not-found__illustration" aria-hidden="true">
          <div className="not-found__orbit not-found__orbit--outer" />
          <div className="not-found__orbit not-found__orbit--inner" />

          <div className="not-found__shield">            
            <svg viewBox="0 0 100 115" fill="none">
              <path
                d="M50 5 88 20v29c0 25-15 44-38 59C27 93 12 74 12 49V20L50 5Z"
                stroke="currentColor"
                strokeWidth="2"
              />

              <path
                d="M50 19 76 29v20c0 18-10 32-26 43C34 81 24 67 24 49V29l26-10Z"
                stroke="currentColor"
                strokeWidth="1"
                strokeDasharray="4 5"
              />

              <path
                d="M37 43a14 14 0 1 1 22 11l10 10"
                stroke="currentColor"
                strokeWidth="3"
                strokeLinecap="round"
              />

              <circle cx="49" cy="43" r="5" stroke="currentColor" strokeWidth="2" />
              <path d="m63 65 6 6" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
            </svg>
          </div>

          <span className="not-found__code not-found__code--top">404</span>
          <span className="not-found__code not-found__code--bottom">NOT FOUND</span>
          <span className="not-found__dot not-found__dot--one" />
          <span className="not-found__dot not-found__dot--two" />
        </div>

        <div className="not-found__message">
          <span className="not-found__eyebrow">
            <span className="not-found__status-dot" /> ERRO 404 · ROTA NÃO LOCALIZADA </span>

          <h1>Parece que esta <br />página <span>não existe.</span>
          </h1>

          <p>
            O endereço acessado não existe ou foi movido.
            Verifique o link ou retorne ao painel para continuar
            sua análise.
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

export default NotFound;