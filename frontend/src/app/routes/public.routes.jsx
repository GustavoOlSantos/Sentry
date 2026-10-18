import Home from "../../features/home";
import NotFound from "../../features/not-found/";

export const publicRoutes = [
  {
    path: "/",
    element: <Home />
  },
  {
    path: "*",
    element: <NotFound />
  }
];