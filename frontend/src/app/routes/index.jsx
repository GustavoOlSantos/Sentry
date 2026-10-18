import { useRoutes } from "react-router-dom";
import { publicRoutes } from "./public.routes";
import { protectedRoutes } from "./protected.routes";

export default function AppRoutes() {
  const routes = useRoutes([
    ...publicRoutes,
    ...protectedRoutes
  ]);

  return routes;
}