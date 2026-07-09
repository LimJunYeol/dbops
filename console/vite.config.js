import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// /api 요청을 Spring(8080)으로 프록시 -> CORS 문제 원천 차단
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})