
import { useLocation } from 'react-router-dom';

import './styles/App.css'

import AppRoutes from './app/routes';
import Header from './components/header'
import Footer from './components/footer'

function App() {
  // a tela de login ocupa a página inteira, sem header nem footer
  const { pathname } = useLocation();
  const isLoginPage = pathname === '/login';

  return (
    <>
      <div className="App">
        {!isLoginPage && <Header/>}
        <AppRoutes />
        {!isLoginPage && <Footer/>}
      </div>
    </>
  )
}

export default App
