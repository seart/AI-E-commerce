import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 的 base 会影响构建后静态资源路径。
// 后台生产部署在 `/admin/` 时，需要通过 VITE_APP_BASE=/admin/ 保证资源地址正确。
function normalizeBase(base?: string) {
  if (!base) {
    return '/'
  }
  return base.endsWith('/') ? base : `${base}/`
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: normalizeBase(env.VITE_APP_BASE),
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  }
})
