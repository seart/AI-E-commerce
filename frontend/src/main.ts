import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import 'normalize.css'
import './style/base.scss'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { useAuthStore } from '@/stores/auth'
import { ENV } from '@/config/env'

// 移动端应用入口：注册 Pinia、路由、全局样式，并恢复本地登录态。
const app = createApp(App)
const pinia = createPinia()

document.title = ENV.appTitle

app.use(pinia)
app.use(router)

// 路由守卫依赖登录态，所以在挂载页面前先恢复一次 session。
useAuthStore(pinia).hydrate()

app.mount('#app')
