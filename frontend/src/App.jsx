
import './styles/App.css'

import AppRoutes from './app/routes';
import Header from './components/header'
import Footer from './components/footer'

function App() {

  return (
    <>
      <div className="App">
        <Header/>
        <AppRoutes />
        <Footer/>
      </div>
    </>
  )
}

export default App
