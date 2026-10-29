import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 10001,
    // Without this Vite silently moves to the next free port when 10001 is busy, and the
    // backend's CORS list only names 10001 — so the page loads but every API call is
    // blocked, which looks like a broken app rather than a busy port. Fail loudly instead.
    strictPort: true,
  },
})
