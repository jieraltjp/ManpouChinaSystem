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
      // Phase 0: 直接指向 manpou-allinone (18090)
      // Phase 1+: 改为 18080 (api-gateway)，由网关统一路由
      '/api': {
        target: 'http://192.168.12.198:18090',
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
