import Home from "../../features/home";
import NotFound from "../../features/not-found/";

import NovoScan from "../../features/scan/novo-scan";
import Hosts from "../../features/hosts";
import Board from "../../features/board";

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
    path: "/hosts",
    element: <Hosts />
  },
   {
    path: "/board",
    element: <Board />
  },
  {
    path: "*",
    element: <NotFound />
  }
];