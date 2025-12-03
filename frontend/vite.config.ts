import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
// 【新增】导入按需导入插件
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers' // 【新增】导入 Element Plus 解析器

export default defineConfig({
  // 【修改 plugins 字段】
  plugins: [
    vue(),
    // 自动导入 Vue API (ref, reactive, computed...)
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: path.resolve(__dirname, 'src/types/auto-imports.d.ts') // 自动生成类型声明文件
    }),
    // 自动导入组件 (el-button, el-table...)
    Components({
      resolvers: [ElementPlusResolver()],
      dts: path.resolve(__dirname, 'src/types/components.d.ts') // 自动生成组件声明文件
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 前端发出的 /api/* 请求，转发到后端 Spring Boot
      // 例如：/api/auth/register -> http://localhost:8081/api/auth/register
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
