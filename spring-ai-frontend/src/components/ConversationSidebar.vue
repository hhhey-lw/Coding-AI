<template>
  <div class="sidebar-container" :class="{ 'collapsed': isCollapsed }">
    <!-- 折叠按钮 -->
    <div class="collapse-btn" @click="toggleCollapse">
      <el-icon>
        <component :is="collapseIcon" />
      </el-icon>
    </div>

    <!-- 侧边栏内容 -->
    <div v-show="!isCollapsed" class="sidebar-content">
      <!-- 标题 -->
      <div class="sidebar-header">
        <div class="back-btn" @click="goToHome" title="返回首页">
          <el-icon>
            <ArrowLeft />
          </el-icon>
        </div>
        <h3>对话历史</h3>
      </div>

      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索对话..."
          :prefix-icon="Search"
          clearable
          size="small"
        />
      </div>

      <!-- 对话列表 -->
      <div class="conversation-list" ref="conversationListRef" @scroll="handleScroll">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ 'active': conversation.id === activeConversationId }"
        >
          <div class="conversation-content" @click="selectConversation(conversation.id)">
            <div class="conversation-title">{{ conversation.title }}</div>
            <div class="conversation-time">{{ formatTime(conversation.updatedAt || conversation.createdAt || '') }}</div>
          </div>
          <el-icon 
            class="delete-icon" 
            @click.stop="handleDeleteConversation(conversation.id)"
            title="删除会话"
          >
            <Delete />
          </el-icon>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore && !loading" class="load-more">
          <el-button text size="small" @click="loadMore">加载更多</el-button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && filteredConversations.length === 0" class="empty-state">
          <el-empty description="暂无对话记录" :image-size="80" />
        </div>
      </div>

      <!-- 新建对话按钮 -->
      <div class="sidebar-footer">
        <el-button type="primary" @click="createNewConversation" style="width: 100%">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, Expand, Fold, Delete, Loading, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const router = useRouter()

// Props
interface Conversation {
  id: string
  title: string
  createdAt?: string
  updatedAt?: string
  time?: string
  status?: string
}

interface Props {
  conversations?: Conversation[]
  activeConversationId?: string
  loading?: boolean
  hasMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  conversations: () => [],
  activeConversationId: '',
  loading: false,
  hasMore: false
})

// Emits
const emit = defineEmits<{
  'select-conversation': [id: string]
  'create-conversation': []
  'toggle-collapse': [collapsed: boolean]
  'delete-conversation': [id: string]
  'load-more': []
  'refresh': []
}>()

// 响应式数据
const isCollapsed = ref(false)
const searchKeyword = ref('')
const conversationListRef = ref<HTMLElement | null>(null)

// 计算属性 - 折叠/展开图标
const collapseIcon = computed(() => isCollapsed.value ? Expand : Fold)

// 计算属性 - 过滤后的对话列表
const filteredConversations = computed(() => {
  if (!searchKeyword.value) {
    return props.conversations
  }
  return props.conversations.filter(conv =>
    conv.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 方法
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  emit('toggle-collapse', isCollapsed.value)
}

const selectConversation = (id: string) => {
  emit('select-conversation', id)
}

const createNewConversation = () => {
  emit('create-conversation')
}

const goToHome = () => {
  router.push('/')
}

const loadMore = () => {
  emit('load-more')
}

const handleDeleteConversation = async (id: string) => {
  try {
    await ElMessageBox.confirm(
      '删除后将无法恢复，确定要删除这个会话吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    emit('delete-conversation', id)
  } catch {
    // 用户取消删除
  }
}

const handleScroll = () => {
  if (!conversationListRef.value || props.loading || !props.hasMore) return
  
  const { scrollTop, scrollHeight, clientHeight } = conversationListRef.value
  // 滚动到底部时自动加载更多
  if (scrollTop + clientHeight >= scrollHeight - 50) {
    loadMore()
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于1天
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 小于7天
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  // 显示具体日期
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.sidebar-container {
  position: relative;
  width: 280px;
  min-width: 280px;
  height: 100%;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.sidebar-container.collapsed {
  width: 50px;
}

.collapse-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
  transition: all 0.3s;
}

.sidebar-container.collapsed .collapse-btn {
  right: 9px;
}

.collapse-btn:hover {
  background: #f0f2f5;
  transform: scale(1.1);
}

.back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  flex-shrink: 0;
}

.back-btn:hover {
  background: #f0f2f5;
  transform: scale(1.1);
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
}

.sidebar-header {
  margin-bottom: 16px;
  padding-right: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.search-box {
  margin-bottom: 16px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.conversation-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
  transform: translateX(4px);
}

.conversation-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.conversation-content {
  flex: 1;
  cursor: pointer;
  overflow: hidden;
}

.delete-icon {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
  flex-shrink: 0;
  margin-left: 8px;
  transition: color 0.3s;
}

.delete-icon:hover {
  color: #f56c6c;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-time {
  font-size: 12px;
  color: #909399;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  color: #909399;
  font-size: 14px;
}

.sidebar-footer {
  padding-top: 8px;
  border-top: 1px solid #e4e7ed;
}

/* 滚动条样式 */
.conversation-list::-webkit-scrollbar {
  width: 6px;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>

