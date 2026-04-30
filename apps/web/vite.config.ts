import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 13000,
    host: '0.0.0.0',   // 监听所有网卡，支持 192.168.12.198 访问
    proxy: {
      // 认证请求：指向 user-service（签发 JWT + 提供公钥）
      // Vite proxy 按配置顺序匹配，/api/v1/auth 比 /api 更具体，优先匹配
      '/api/v1/auth': {
        target: 'http://localhost:18081',
        changeOrigin: true,
      },
      // 业务请求：指向 allinone（所有业务 API）
      // 生产环境：改为 18080 (api-gateway)，由网关统一路由
      '/api': {
        target: 'http://localhost:18090',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  build: {
    target: 'es2022',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
        },
      },
    },
  },
})
