import ProtectedRoute from "./ProtectedRoute";

import { ROUTE_ROLES } from "../../features/auth/roles";

import Home from "../../features/home";
import NovoScan from "../../features/scan/novo-scan";
import Hosts from "../../features/hosts";
import Board from "../../features/board";
import Users from "../../features/users";
import Profile from "../../features/auth/profile";

/**
 * Envolve uma página com login + perfis permitidos (definidos em ROUTE_ROLES).
 * Para proteger uma nova rota: { path: "/x", element: guard("/x", <X />) }
 * e adicione "/x" em ROUTE_ROLES (features/auth/roles.js).
 */
export const guard = (path, element) => (
  <ProtectedRoute roles={ROUTE_ROLES[path]}>{element}</ProtectedRoute>
);

export const protectedRoutes = [
  { path: "/",          element: guard("/", <Home />) },
  { path: "/hosts",     element: guard("/hosts", <Hosts />) },
  { path: "/board",     element: guard("/board", <Board />) },
  { path: "/novo-scan", element: guard("/novo-scan", <NovoScan />) },
  { path: "/usuarios",  element: guard("/usuarios", <Users />) },
  { path: "/perfil",    element: guard("/perfil", <Profile />) },
];
