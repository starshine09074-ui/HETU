import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const codespaceHost = process.env.CODESPACE_NAME
  ? `${process.env.CODESPACE_NAME}-5173.app.github.dev`
  : null

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    allowedHosts: ['localhost', '127.0.0.1', ...(codespaceHost ? [codespaceHost] : [])],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      }
    }
  }
})
