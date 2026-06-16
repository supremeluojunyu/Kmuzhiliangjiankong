import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.uqm.qualitymonitor',
  appName: '昆明学院质量监控任务管理系统',
  webDir: 'dist',
  server: {
    // http  scheme，避免 https 页面请求 http API 被 WebView 混合内容策略拦截
    androidScheme: 'http',
    cleartext: true,
  },
  plugins: {
    CapacitorHttp: {
      enabled: true,
    },
  },
};

export default config;
