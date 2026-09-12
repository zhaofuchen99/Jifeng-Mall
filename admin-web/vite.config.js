import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端网关地址。业务接口都是 /api 开头，图片是 /upload 开头，两者都代理到网关。
const GATEWAY = 'http://localhost:8888'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 前台 mall-web 占 5173，后台用 5174，两个可以同时开着联调
    port: 5174,
    host: true,
    proxy: {
      '/api': { target: GATEWAY, changeOrigin: true },
      '/upload': { target: GATEWAY, changeOrigin: true }
    }
  }
})
