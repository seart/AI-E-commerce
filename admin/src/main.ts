import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'normalize.css'
import 'element-plus/dist/index.css'
import './styles/admin.css'
import App from './App.vue'
import router from './router'
import { useSessionStore } from '@/stores/session'

// 后台应用入口：注册 Element Plus、Pinia、路由，并恢复后台登录态。
const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus)

// 刷新后台页面后从 localStorage 恢复 token 和用户信息。
useSessionStore(pinia).hydrate()

app.use(router)
app.mount('#app')
