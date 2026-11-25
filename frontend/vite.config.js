import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, /api se reenvia al nginx de Docker Compose (puerto 8000) o a la URL de API_URL.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': process.env.API_URL || 'http://localhost:8000' },
  },
})
