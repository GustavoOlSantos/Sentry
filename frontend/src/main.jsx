import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { BrowserRouter } from 'react-router-dom';
import ScrollToTop from './app/routes/scrollToTop.jsx';
import { AuthProvider } from './features/auth/AuthProvider.jsx';

import './styles/index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
    <AuthProvider>
      <ScrollToTop />
      <App />
    </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
