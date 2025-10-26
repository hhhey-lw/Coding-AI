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
      <div class="conversation-list">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ 'active': conversation.id === activeConversationId }"
          @click="selectConversation(conversation.id)"
        >
          <div class="conversation-title">{{ conversation.title }}</div>
          <div class="conversation-time">{{ formatTime(conversation.time) }}</div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredConversations.length === 0" class="empty-state">
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
import { Search, Plus, Expand, Fold } from '@element-plus/icons-vue'

// Props
interface Conversation {
  id: string
  title: string
  time: string
}

interface Props {
  conversations?: Conversation[]
  activeConversationId?: string
}

const props = withDefaults(defineProps<Props>(), {
  conversations: () => [],
  activeConversationId: ''
})

// Emits
const emit = defineEmits<{
  'select-conversation': [id: string]
  'create-conversation': []
  'toggle-collapse': [collapsed: boolean]
}>()

// 响应式数据
const isCollapsed = ref(false)
const searchKeyword = ref('')

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

const formatTime = (time: string) => {
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

.sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
}

.sidebar-header {
  margin-bottom: 16px;
  padding-right: 40px;
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
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
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

