import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxy routes to backend at localhost:5000
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:5000'
    }
  }
})
