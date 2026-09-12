import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端网关地址。所有业务接口都以 /api 开头，图片是 /upload 开头，两者都代理到网关。
const GATEWAY = 'http://localhost:8888'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 走代理而不是直连 :8888 的原因：浏览器只跟 vite 同源通信，彻底绕开 CORS，
      // 也不用管网关 CORS 配置里没有 PATCH/HEAD 这件事。
      '/api': { target: GATEWAY, changeOrigin: true },
      // 注意：网关和每个服务都要 /api 前缀，**不能** rewrite 掉。
      '/upload': { target: GATEWAY, changeOrigin: true }
    }
  }
})
