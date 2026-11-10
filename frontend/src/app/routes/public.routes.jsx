import NotFound from "../../features/not-found/";
import Login from "../../features/auth/login";

export const publicRoutes = [
  {
    path: "/login",
    element: <Login />
  },
  {
    path: "*",
    element: <NotFound />
  }
];
