import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'normalize.css'
import 'element-plus/dist/index.css'
import './styles/admin.css'
import App from './App.vue'
import router from './router'
import { useSessionStore } from '@/stores/session'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus)

useSessionStore(pinia).hydrate()

app.use(router)
app.mount('#app')
