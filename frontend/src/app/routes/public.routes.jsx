import Home from "../../features/home";
import NotFound from "../../features/not-found/";

import NovoScan from "../../features/scan/novo-scan";

export const publicRoutes = [
  {
    path: "/",
    element: <Home />
  },
  {
    path: "/novo-scan",
    element: <NovoScan />
  },
  {
    path: "*",
    element: <NotFound />
  }
];