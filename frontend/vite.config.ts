import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // Everything the browser calls on the backend lives under /api (context-path),
      // including the OIDC login/callback endpoints. Forward the same X-Forwarded-*
      // headers Traefik sends in prod so Spring (forward-headers-strategy: framework)
      // builds OIDC redirect URIs — and the post-login redirect — against the SPA
      // origin (localhost:5173), not the backend's own host:port.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
        headers: {
          'X-Forwarded-Host': 'localhost:5173',
          'X-Forwarded-Proto': 'http',
          'X-Forwarded-Port': '5173',
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
  },
})
