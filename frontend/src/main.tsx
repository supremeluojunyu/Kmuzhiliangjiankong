import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.tsx';
import { isMobileApp } from '@/utils/app';
import { ensureDefaultServerConfig } from '@/utils/serverConfig';

if (isMobileApp()) {
  document.body.classList.add('mobile-app');
  document.documentElement.classList.add('mobile-app');
  ensureDefaultServerConfig();
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
