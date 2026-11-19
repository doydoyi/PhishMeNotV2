import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// In development the React app calls the Spring Boot API through this proxy, so the
// session cookie and CSRF cookie are first-party for the browser.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8080';
  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': { target: backend, changeOrigin: false },
      },
    },
  };
});
