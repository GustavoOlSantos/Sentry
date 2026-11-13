import ProtectedRoute from "./ProtectedRoute";

import { ROUTE_ROLES } from "../../features/auth/roles";

import Home from "../../features/home";
import NovoScan from "../../features/scan/novo-scan";
import Hosts from "../../features/hosts";
import Board from "../../features/board";
import ScanGroups from "../../features/scans";
import GroupDetail from "../../features/scans/group-detail";
import ScanDetail from "../../features/scans/scan-detail";
import Users from "../../features/users";
import Profile from "../../features/auth/profile";
import Policies from "../../features/policies";
import PolicyDetail from "../../features/policies/detail";
import NewPolicy from "../../features/policies/new";

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
  { path: "/scans",                 element: guard("/scans", <ScanGroups />) },
  { path: "/scans/grupo/:groupId",  element: guard("/scans/grupo/:groupId", <GroupDetail />) },
  { path: "/scans/:scanId",         element: guard("/scans/:scanId", <ScanDetail />) },
  { path: "/usuarios",  element: guard("/usuarios", <Users />) },
  { path: "/perfil",    element: guard("/perfil", <Profile />) },
  { path: "/policies",       element: guard("/policies", <Policies />) },
  { path: "/policies/nova",  element: guard("/policies/nova", <NewPolicy />) },
  { path: "/policies/:id",   element: guard("/policies", <PolicyDetail />) },
];
