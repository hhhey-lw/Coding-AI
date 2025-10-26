import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

// 导入页面组件
import Index from '../views/Index.vue'
import WorkflowDesigner from '../components/workflow/WorkflowDesigner.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Chat from '../views/Chat.vue'

// 定义路由配置
const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { requiresAuth: false, title: '注册' }
  },
  {
    path: '/',
    name: 'Index',
    component: Index,
    meta: { requiresAuth: true, title: '首页' }
  },
  {
    path: '/workflow',
    name: 'WorkflowDesigner',
    component: WorkflowDesigner,
    meta: { requiresAuth: true, title: '工作流设计器' }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: Chat,
    meta: { requiresAuth: true, title: 'AI 聊天助手' }
  },
  {
    path: '/chat-legacy',
    name: 'ChatLegacy',
    component: Chat,
    meta: { requiresAuth: true, title: 'AI 聊天助手（旧版）' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const isLoggedIn = authStore.isLoggedIn
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - Spring AI Workflow`
  }
  
  // 需要登录的页面
  if (to.meta.requiresAuth) {
    if (isLoggedIn) {
      next()
    } else {
      ElMessage.warning('请先登录')
      next('/login')
    }
  } 
  // 登录/注册页面
  else if (to.path === '/login' || to.path === '/register') {
    if (isLoggedIn) {
      // 已登录则跳转到首页
      next('/')
    } else {
      next()
    }
  }
  // 其他页面
  else {
    next()
  }
})

export default router
