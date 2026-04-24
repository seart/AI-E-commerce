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

const app = createApp(App)
const pinia = createPinia()

document.title = ENV.appTitle

app.use(pinia)
app.use(router)

useAuthStore(pinia).hydrate()

app.mount('#app')
