<template>
  <div class="node-palette-container">
    <!-- Search Bar -->
    <div class="palette-header">
      <h4 class="palette-title">Add Nodes</h4>
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="Search nodes"
          prefix-icon="Search"
          clearable
          @clear="searchQuery = ''"
        />
      </div>
    </div>
    <div class="palette-divider"></div>

    <!-- Scrollable Content -->
    <el-scrollbar class="palette-content">
      <el-collapse v-model="activeNames">
        <el-collapse-item title="Agent Flows" name="1">
          <div class="node-list">
            <div 
              v-for="node in filteredNodes" 
              :key="node.type" 
              class="node-item"
              draggable="true"
              @dragstart="onDragStart($event, node)"
              @click="onNodeItemClick($event, node)"
            >
              <div class="node-icon-wrapper">
                <el-icon :size="24" :color="node.iconColor">
                  <component :is="node.icon" />
                </el-icon>
              </div>
              <div class="node-info">
                <div class="node-name">{{ node.label }}</div>
                <div class="node-desc">{{ node.description }}</div>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-scrollbar>
    
    <!-- 添加节点确认悬浮框 -->
    <div 
      v-if="showAddPopup" 
      class="add-popup"
    >
      <div class="popup-content">
        <div class="popup-header">
          <span>添加节点</span>
          <el-icon class="close-icon" @click="showAddPopup = false"><Close /></el-icon>
        </div>
        <div class="popup-node-info">
          <div class="popup-icon" :style="{ backgroundColor: selectedNodeForAdd?.iconColor }">
            <el-icon :size="24" color="white">
              <component :is="selectedNodeForAdd?.icon" />
            </el-icon>
          </div>
          <div>
            <div class="popup-node-name">{{ selectedNodeForAdd?.label }}</div>
            <div class="popup-node-desc">{{ selectedNodeForAdd?.description }}</div>
          </div>
        </div>
        <el-button type="primary" size="default" @click="confirmAddNode" class="add-btn">
          添加到画布
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Search,
  VideoPlay,
  Share,
  Cpu,
  MagicStick,
  User,
  ChatDotRound,
  Connection,
  Files,
  Tools,
  ArrowRight,
  Refresh,
  Link,
  Message,
  Document,
  Close
} from '@element-plus/icons-vue'

const searchQuery = ref('')
const activeNames = ref(['1'])

// Define available nodes matching the user's request and project context
const availableNodes = [
  {
    type: 'agent',
    label: 'Agent',
    description: '在运行时动态选择和使用工具',
    icon: 'Cpu',
    iconColor: '#4DD0E1',
    data: { theme: 'theme-cyan', color: '#22d3ee', icon: 'Cpu' }
  },
  {
    type: 'condition-basic', // Mapping to Condition
    label: 'Condition',
    description: '基于条件选择某条路径执行',
    icon: 'Share', 
    iconColor: '#FFB938',
    data: { theme: 'theme-orange', color: '#f59e0b', icon: 'Connection' }
  },
  {
    type: 'condition-agent',
    label: 'Condition Agent',
    description: '利用LLM根据场景智能选择某条路径',
    icon: 'Share',
    iconColor: '#ff8fab',
    data: { theme: 'theme-pink', color: '#fb7185', icon: 'Share' }
  },
  // Custom Function (Not in current AgentFlow example, skipping or adding placeholder)
  {
    type: 'reply',
    label: 'Direct Reply',
    description: '结束节点，直接回复用户请求',
    icon: 'ChatDotRound',
    iconColor: '#4DDBBB',
    data: { theme: 'theme-teal', color: '#2dd4bf', icon: 'ChatDotRound' }
  },
  // Execute Flow (Skipping)
  // HTTP (Skipping)
  // Human Input (Hidden)
  // {
  //   type: 'human',
  //   label: 'Human Input',
  //   description: '在执行期间请求人工输入、批准或拒绝',
  //   icon: 'User',
  //   iconColor: '#6E6EFD',
  //   data: { theme: 'theme-purple', color: '#818cf8', icon: 'User' }
  // },
  // Iteration (Skipping)
  {
    type: 'llm',
    label: 'LLM',
    description: '大型语言模型，文本补全',
    icon: 'MagicStick',
    iconColor: '#64B5F6',
    data: { theme: 'theme-blue', color: '#60a5fa', icon: 'MagicStick' }
  },
  // Loop (Skipping)
  {
    type: 'retriever',
    label: 'Retriever',
    description: '从知识库中检索信息',
    icon: 'Files',
    iconColor: '#b8bedd',
    data: { theme: 'theme-slate', color: '#9ca3af', icon: 'Files' }
  },
  {
    type: 'start',
    label: 'Start',
    description: '开始节点，表示流程的起点',
    icon: 'VideoPlay',
    iconColor: '#7EE787',
    data: { theme: 'theme-green', color: '#4ade80', icon: 'VideoPlay' }
  },
  // Sticky Note (Skipping)
  {
    type: 'tool',
    label: 'Tool',
    description: '工具节点，调用的外部工具或服务',
    icon: 'Tools',
    iconColor: '#d4a373',
    data: { theme: 'theme-brown', color: '#d97706', icon: 'Tools' }
  }
]

const filteredNodes = computed(() => {
  if (!searchQuery.value) return availableNodes
  const query = searchQuery.value.toLowerCase()
  return availableNodes.filter(node => 
    node.label.toLowerCase().includes(query) || 
    node.description.toLowerCase().includes(query)
  )
})

const onDragStart = (event: DragEvent, node: any) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/json', JSON.stringify(node))
    event.dataTransfer.effectAllowed = 'move'
  }
}

// 悬浮框状态
const showAddPopup = ref(false)
const selectedNodeForAdd = ref<any>(null)

// 点击节点项
const onNodeItemClick = (event: MouseEvent, node: any) => {
  // 检测是否是移动端
  const isMobile = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  
  if (isMobile) {
    event.stopPropagation()
    selectedNodeForAdd.value = node
    showAddPopup.value = true
  }
}

// 确认添加节点
const confirmAddNode = () => {
  if (selectedNodeForAdd.value) {
    // 触发自定义事件，通知父组件添加节点
    const event = new CustomEvent('add-node', {
      detail: selectedNodeForAdd.value,
      bubbles: true
    })
    document.querySelector('.node-palette-container')?.dispatchEvent(event)
    
    showAddPopup.value = false
    selectedNodeForAdd.value = null
  }
}
</script>

<style scoped>
.node-palette-container {
  width: 100%;
  height: 100%;
  background: white;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.palette-header {
  padding: 16px;
}

.palette-title {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.palette-divider {
  height: 1px;
  background: #e5e7eb;
  width: 100%;
}

.palette-content {
  flex: 1;
}

.node-list {
  padding: 0 16px;
}

.node-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  cursor: grab;
  border-bottom: 1px solid #f3f4f6;
  transition: background-color 0.2s;
}

.node-item:hover {
  background-color: #f9fafb;
}

.node-item:last-child {
  border-bottom: none;
}

.node-icon-wrapper {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.node-info {
  flex: 1;
}

.node-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
}

.node-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

:deep(.el-collapse) {
  border: none;
}

:deep(.el-collapse-item__header) {
  padding: 0 16px;
  font-weight: 600;
  color: #374151;
  background-color: transparent;
}

:deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

/* 添加节点悬浮框 */
.add-popup {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2000;
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 250px;
  border: 2px solid #6200ee;
  animation: popupZoomIn 0.25s ease-out;
}

@keyframes popupZoomIn {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.popup-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.popup-header .close-icon {
  font-size: 20px;
  cursor: pointer;
  color: #6b7280;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.popup-header .close-icon:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.popup-node-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 12px;
}

.popup-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.popup-node-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.popup-node-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.add-btn {
  width: 100%;
  height: 44px;
  background: #6200ee;
  border-color: #6200ee;
  font-weight: 600;
  font-size: 15px;
  border-radius: 10px;
}

.add-btn:hover {
  background: #5b00dc;
  border-color: #5b00dc;
}
</style>
