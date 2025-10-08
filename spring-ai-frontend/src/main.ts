import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
const pinia = createPinia()

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用 Pinia 状态管理
app.use(pinia)

// 配置 ElMessage 全局默认偏移量（从底部往上60px）
app.use(ElementPlus, {
  message: {
    offset: 60
  }
})

app.use(router).mount('#app')

// 初始化 Auth Store（从本地存储加载数据）
import { useAuthStore } from './stores/auth'
const authStore = useAuthStore()
authStore.init()
