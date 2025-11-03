<template>
  <div class="index-container">
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <!-- 居中的导航按钮 -->
      <div class="nav-buttons">
        <el-button
          :class="{ active: currentTab === 'explore' }"
          @click="currentTab = 'explore'"
          text
        >
          探索
        </el-button>
        <el-button
          :class="{ active: currentTab === 'workbench' }"
          @click="currentTab = 'workbench'"
          text
        >
          工作台
        </el-button>
        <el-button
          :class="{ active: currentTab === 'knowledge' }"
          @click="currentTab = 'knowledge'"
          text
        >
          知识库
        </el-button>
      </div>

      <!-- 右上角用户信息 -->
      <div class="user-info">
        <el-dropdown @command="handleUserCommand">
          <div class="user-avatar-wrapper">
            <el-avatar :size="40" :src="userInfo?.userAvatar || undefined">
              <el-icon><User /></el-icon>
            </el-avatar>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account">
                <el-icon><User /></el-icon>
                账户
              </el-dropdown-item>
              <el-dropdown-item command="about">
                <el-icon><InfoFilled /></el-icon>
                关于
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                登出
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 探索页 -->
      <div v-show="currentTab === 'explore'" class="tab-content">
        <div class="explore-header">
          <h2>Agent 智能体</h2>
          <p class="explore-subtitle">探索强大的AI智能助手，帮助您完成各种任务</p>
        </div>

        <!-- Agent卡片网格 -->
        <div class="agent-grid">
          <!-- React Agent 卡片 -->
          <div class="agent-card" @click="handleOpenAgent('react')">
            <div class="agent-card-icon">
              <el-icon :size="40" color="#409eff"><ChatDotRound /></el-icon>
            </div>
            <div class="agent-card-content">
              <h3>React Agent</h3>
              <p class="agent-description">
                基于ReAct架构的智能对话助手，能够理解上下文并通过工具调用完成复杂任务
              </p>
              <div class="agent-features">
                <span class="feature-tag">对话理解</span>
                <span class="feature-tag">工具调用</span>
                <span class="feature-tag">上下文记忆</span>
              </div>
            </div>
          </div>

          <!-- Plan-Execute Agent 卡片 -->
          <div class="agent-card" @click="handleOpenAgent('plan-execute')">
            <div class="agent-card-icon">
              <el-icon :size="40" color="#67c23a"><Operation /></el-icon>
            </div>
            <div class="agent-card-content">
              <h3>Plan-Execute Agent</h3>
              <p class="agent-description">
                规划执行型智能助手，擅长将复杂任务分解为步骤，逐步执行并生成高质量结果
              </p>
              <div class="agent-features">
                <span class="feature-tag">任务规划</span>
                <span class="feature-tag">分步执行</span>
                <span class="feature-tag">进度追踪</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 工作台 -->
      <div v-show="currentTab === 'workbench'" class="tab-content">
        <div class="workbench-header">
          <h2>我的工作流</h2>
          <el-button type="primary" @click="handleCreateWorkflow">
            <el-icon><Plus /></el-icon>
            新建工作流
          </el-button>
        </div>

        <!-- 搜索 -->
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索工作流名称..."
            clearable
            style="width: 300px"
            @keyup.enter="loadWorkflowList"
            @change="loadWorkflowList"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="loadWorkflowList">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>

        <!-- 工作流列表 -->
        <div v-loading="loading" class="workflow-list">
          <div v-if="workflowList.length === 0" class="empty-state">
            <el-icon :size="60" color="#909399"><DocumentCopy /></el-icon>
            <p>暂无工作流，点击"新建工作流"开始创建</p>
          </div>

          <div v-else class="workflow-grid">
            <div
              v-for="workflow in workflowList"
              :key="workflow.id"
              class="workflow-card"
            >
              <div class="workflow-card-header">
                <h3 @click="handleOpenWorkflow(workflow)">{{ workflow.name }}</h3>
                <el-dropdown trigger="click" @command="(cmd: string) => handleCardAction(cmd, workflow)">
                  <el-icon class="card-menu-icon"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="delete">
                        <el-icon><CircleClose /></el-icon>
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <p class="workflow-description" @click="handleOpenWorkflow(workflow)">
                {{ workflow.description || '暂无描述' }}
              </p>
              <div class="workflow-meta" @click="handleOpenWorkflow(workflow)">
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatTime(workflow.updateTime) }}
                </span>
                <span class="meta-item">
                  <el-icon><Grid /></el-icon>
                  版本 {{ workflow.version }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="workflowList.length > 0" class="pagination-wrapper">
          <div class="pagination-info">
            <span>共 {{ total }} 个工作流</span>
          </div>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="loadWorkflowList"
          />
        </div>
      </div>

      <!-- 知识库 -->
      <div v-show="currentTab === 'knowledge'" class="tab-content">
        <div class="empty-state">
          <el-icon :size="80" color="#909399"><Reading /></el-icon>
          <h2>暂未开发</h2>
          <p>知识库功能正在开发中，敬请期待...</p>
        </div>
      </div>
    </div>

    <!-- 账户信息弹窗 -->
    <el-dialog v-model="showAccountDialog" title="账户信息" width="400px" center>
      <div class="account-info">
        <div class="account-avatar">
          <el-avatar :size="80" :src="userInfo?.userAvatar || undefined">
            <el-icon :size="40"><User /></el-icon>
          </el-avatar>
        </div>
        <div class="account-details">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">
              {{ userInfo?.userName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ userInfo?.email || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { AuthAPI } from '@/api/auth'
import WorkflowAPI, { type WorkflowConfigVO } from '@/api/workflow'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const currentTab = ref('explore')
const showAccountDialog = ref(false)
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const workflowList = ref<WorkflowConfigVO[]>([])

// 计算属性
const userInfo = computed(() => authStore.userInfo)

// 加载工作流列表
const loadWorkflowList = async () => {
  loading.value = true
  
  try {
    const response = await WorkflowAPI.getMyWorkflowList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      workflowName: searchKeyword.value || undefined,
      status: undefined
    })

    if (response.code === 1 && response.data) {
      workflowList.value = response.data.list || []
      total.value = response.data.total ? parseInt(response.data.total) : 0
      console.log('工作流列表加载成功, 总数:', total.value, '当前页数据:', workflowList.value.length)
    } else {
      ElMessage.error(response.message || '加载工作流列表失败')
    }
  } catch (error) {
    console.error('加载工作流列表失败:', error)
    ElMessage.error('加载工作流列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (timeStr: string) => {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 1分钟内
  if (diff < 60000) {
    return '刚刚'
  }
  // 1小时内
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 今天
  if (date.toDateString() === now.toDateString()) {
    return `今天 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  // 昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return `昨天 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  // 其他
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 用户菜单命令处理
const handleUserCommand = (command: string) => {
  switch (command) {
    case 'account':
      showAccountDialog.value = true
      break
    case 'about':
      ElMessage.info('暂未开放')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 登出
const handleLogout = async () => {
  ElMessageBox.confirm('确定要退出登录吗？', '确认登出', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const refreshToken = authStore.getRefreshToken()
      if (refreshToken) {
        // 调用登出接口
        await AuthAPI.logout({ refreshToken })
      }
    } catch (error) {
      console.error('登出接口调用失败:', error)
    } finally {
      // 无论接口成功与否，都清除本地token
      authStore.clearAuth()
      ElMessage.success('已退出登录')
      router.push('/login')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 新建工作流
const handleCreateWorkflow = () => {
  router.push('/workflow')
}

// 打开工作流
const handleOpenWorkflow = (workflow: WorkflowConfigVO) => {
  // 跳转到工作流编辑器，传递工作流ID
  router.push({
    path: '/workflow',
    query: { id: workflow.id }
  })
}

// 打开Agent
const handleOpenAgent = (type: string) => {
  router.push({
    path: '/chat',
    query: { model: type }
  })
}

// 处理卡片操作
const handleCardAction = async (command: string, workflow: WorkflowConfigVO) => {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定要禁用工作流 "${workflow.name}" 吗？禁用后可以重新启用。`,
        '确认禁用',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      // 调用更新状态API（设置为禁用）
      const response = await WorkflowAPI.updateWorkflowStatus(workflow.id, 0)
      if (response.code === 1) {
        ElMessage.success('工作流已禁用')
        // 重新加载列表
        loadWorkflowList()
      } else {
        ElMessage.error(response.message || '操作失败')
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('更新工作流状态失败:', error)
        ElMessage.error('操作失败')
      }
    }
  }
}

// 监听tab切换，切换到工作台时加载工作流列表
watch(currentTab, (newTab) => {
  if (newTab === 'workbench' && workflowList.value.length === 0) {
    loadWorkflowList()
  }
})

// 组件挂载时，如果默认是工作台则加载数据
onMounted(() => {
  if (currentTab.value === 'workbench') {
    loadWorkflowList()
  }
})
</script>

<style scoped>
.index-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 顶部导航栏 */
.top-navbar {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: relative;
}

.nav-buttons {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.nav-buttons .el-button {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  padding: 8px 24px;
  transition: all 0.3s;
}

.nav-buttons .el-button.active {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 600;
}

.nav-buttons .el-button:hover {
  color: #409eff;
  background: #ecf5ff;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar-wrapper {
  cursor: pointer;
  transition: transform 0.2s;
}

.user-avatar-wrapper:hover {
  transform: scale(1.05);
}

/* 主内容区域 */
.main-content {
  flex: 1;
  overflow: hidden;
}

.tab-content {
  height: 100%;
  padding: 24px;
  padding-bottom: 80px; /* 为固定在底部的分页组件留出空间 */
  overflow-y: auto;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-state h2 {
  margin: 16px 0 8px 0;
  font-size: 24px;
  font-weight: 500;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 探索页 */
.explore-header {
  margin-bottom: 32px;
}

.explore-header h2 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.explore-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.agent-card {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  gap: 20px;
}

.agent-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.agent-card-icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  border-radius: 12px;
}

.agent-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.agent-card-content h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  transition: color 0.2s;
}

.agent-card:hover .agent-card-content h3 {
  color: #409eff;
}

.agent-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  flex: 1;
}

.agent-features {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.feature-tag {
  display: inline-block;
  padding: 4px 12px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  transition: all 0.2s;
}

.agent-card:hover .feature-tag {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

/* 工作台 */
.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.workbench-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.workflow-list {
  min-height: 400px;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.workflow-card {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
}

.workflow-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.workflow-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.workflow-card-header h3 {
  flex: 1;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s;
}

.workflow-card-header h3:hover {
  color: #409eff;
}

.card-menu-icon {
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.card-menu-icon:hover {
  color: #303133;
  background: #f5f7fa;
}

.workflow-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 42px;
  cursor: pointer;
}

.workflow-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  cursor: pointer;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 分页样式 */
.pagination-wrapper {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 12px 24px;
  max-width: 600px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.pagination-info {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.pagination-info span {
  display: inline-block;
  padding: 6px 16px;
  background: #f0f2f5;
  border-radius: 8px;
}

:deep(.el-pagination) {
  display: flex;
  justify-content: center;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #409eff;
  color: white;
}

:deep(.el-pagination.is-background .btn-next),
:deep(.el-pagination.is-background .btn-prev) {
  background-color: white;
}

:deep(.el-pagination.is-background .btn-next:disabled),
:deep(.el-pagination.is-background .btn-prev:disabled) {
  background-color: #f5f7fa;
}

/* 账户信息弹窗 */
.account-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.account-avatar {
  margin-bottom: 24px;
}

.account-details {
  width: 100%;
}

/* 滚动条样式 */
.tab-content::-webkit-scrollbar {
  width: 8px;
}

.tab-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.tab-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.tab-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
