import { Navigate, useLocation } from "react-router-dom";

import { useAuth } from "../../features/auth/useAuth";
import Forbidden from "../../features/forbidden";

/**
 * Protege uma rota.
 *  - sem login           → redireciona para /login (e volta para cá depois de entrar);
 *  - perfil não permitido → mostra a tela de acesso negado, mantendo a URL;
 *  - `roles` omitido      → basta estar logado.
 *
 * Use o helper `guard(path, <Pagina />)` de protected.routes.jsx, que já pega
 * os perfis de ROUTE_ROLES.
 */
export default function ProtectedRoute({ children, roles }) {
  const { user, initializing } = useAuth();
  const location = useLocation();

  if (initializing) {
    return (
      <main className="auth-loading" aria-busy="true">
        <i className="fa-solid fa-circle-notch fa-spin" />
        Verificando sessão...
      </main>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles?.length && !roles.includes(user.role)) {
    return <Forbidden />;
  }

  return children;
}
