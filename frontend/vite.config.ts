import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import pkg from './package.json'

export default defineConfig({
  plugins: [react()],
  define: {
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(pkg.version),
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            const remote = req.socket?.remoteAddress?.replace(/^::ffff:/, '');
            const incomingXff = req.headers['x-forwarded-for'];
            const incomingRealIp = req.headers['x-real-ip'];
            if (incomingXff) {
              proxyReq.setHeader('X-Forwarded-For', String(incomingXff));
            } else if (remote) {
              proxyReq.setHeader('X-Forwarded-For', remote);
            }
            if (incomingRealIp) {
              proxyReq.setHeader('X-Real-IP', String(incomingRealIp));
            } else if (remote) {
              proxyReq.setHeader('X-Real-IP', remote);
            }
          });
        },
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          antd: ['antd', '@ant-design/icons'],
          echarts: ['echarts', 'echarts-for-react'],
          quill: ['quill'],
        },
      },
    },
    chunkSizeWarningLimit: 600,
  },
})
