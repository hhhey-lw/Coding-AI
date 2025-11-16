<template>
  <div class="node-palette">
    <div class="palette-header">
      <h3>节点库</h3>
      <el-input
        v-model="searchText"
        placeholder="搜索节点..."
        size="small"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="palette-content">
      <el-collapse v-model="activeCategories" accordion>
        <el-collapse-item 
          v-for="category in filteredCategories" 
          :key="category.name"
          :title="category.name" 
          :name="category.name"
        >
          <div class="node-list">
            <div
              v-for="nodeType in category.nodes"
              :key="nodeType.type"
              class="node-item"
              :draggable="true"
              @dragstart="handleDragStart($event, nodeType)"
              @click="$emit('node-select', nodeType)"
            >
              <div class="node-icon">
                <el-icon :size="20">
                  <component :is="nodeType.icon" />
                </el-icon>
              </div>
              <div class="node-info">
                <div class="node-title">{{ nodeType.name }}</div>
                <div class="node-desc">{{ nodeType.description }}</div>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NodeType, NodeTypeDescriptions } from '@/types/workflow'

// Props和Emits
defineEmits<{
  'node-select': [nodeType: NodeTypeInfo]
  'node-drag': [nodeType: NodeTypeInfo]
}>()

// 响应式数据
const searchText = ref('')
const activeCategories = ref(['基础节点'])

// 节点类型信息接口
interface NodeTypeInfo {
  type: string
  name: string
  description: string
  icon: string
  category: string
}

// 节点分类配置
const nodeCategories = ref([
  {
    name: '基础节点',
    nodes: [
      {
        type: NodeType.START,
        name: '开始',
        description: '工作流入口节点',
        icon: 'VideoPlay',
        category: '基础节点'
      },
      {
        type: NodeType.END,
        name: '结束',
        description: '工作流出口节点',
        icon: 'VideoPause',
        category: '基础节点'
      },
      {
        type: NodeType.JUDGE,
        name: '条件',
        description: '逻辑判断节点',
        icon: 'Switch',
        category: '工具节点'
      }
    ]
  },
  {
    name: 'AI处理',
    nodes: [
      {
        type: NodeType.LLM,
        name: '大模型',
        description: '大语言模型处理',
        icon: 'ChatDotRound',
        category: 'AI处理'
      },
      {
        type: NodeType.IMG_GEN,
        name: '图像生成',
        description: 'AI图像生成',
        icon: 'Picture',
        category: 'AI处理'
      },
      {
        type: NodeType.VIDEO_GEN,
        name: '视频生成',
        description: 'AI视频生成',
        icon: 'VideoCamera',
        category: 'AI处理'
      },
      {
        type: NodeType.MUSIC_GEN,
        name: '音乐生成',
        description: 'AI音乐生成',
        icon: 'Headphone',
        category: 'AI处理'
      }
    ]
  },
  {
    name: '工具节点',
    nodes: [
      {
        type: NodeType.MCP,
        name: 'MCP服务',
        description: 'MCP服务调用',
        icon: 'Connection',
        category: '工具节点'
      },
      {
        type: NodeType.SCRIPT,
        name: '脚本执行',
        description: 'JavaScript/Python脚本',
        icon: 'DocumentCopy',
        category: '工具节点'
      },
      {
        type: NodeType.EMAIL,
        name: '发送邮件',
        description: '邮件发送节点',
        icon: 'Message',
        category: '工具节点'
      }
    ]
  }
])

// 计算属性：过滤后的分类
const filteredCategories = computed(() => {
  if (!searchText.value) {
    return nodeCategories.value
  }
  
  const filtered = nodeCategories.value.map(category => ({
    ...category,
    nodes: category.nodes.filter(node => 
      node.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
      node.description.toLowerCase().includes(searchText.value.toLowerCase())
    )
  })).filter(category => category.nodes.length > 0)
  
  return filtered
})

// 拖拽开始处理
const handleDragStart = (event: DragEvent, nodeType: NodeTypeInfo) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/json', JSON.stringify({
      type: nodeType.type,
      name: nodeType.name,
      description: nodeType.description
    }))
    event.dataTransfer.effectAllowed = 'copy'
  }
}
</script>

<style scoped>
/* OpenAI 风格 - 简洁、现代、扁平化 */
.node-palette {
  width: 300px;
  height: 100%;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.palette-header {
  padding: 20px 16px 16px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: white;
}

.palette-header h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  letter-spacing: -0.01em;
}

.palette-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.node-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.node-item:hover {
  border-color: #d1d5db;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.node-item:active {
  cursor: grabbing;
  transform: scale(0.98);
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.node-icon {
  margin-right: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: #f3f4f6;
  border-radius: 8px;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.node-item:hover .node-icon {
  background: #e5e7eb;
  color: #374151;
}

.node-info {
  flex: 1;
  min-width: 0;
}

.node-title {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 2px;
  letter-spacing: -0.01em;
}

.node-desc {
  font-size: 11px;
  color: #6b7280;
  line-height: 1.4;
  word-break: break-all;
}

:deep(.el-collapse) {
  border: none;
}

:deep(.el-collapse-item__header) {
  height: 36px;
  line-height: 36px;
  background: transparent;
  border: none;
  font-weight: 600;
  font-size: 12px;
  color: #6b7280;
  padding: 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  transition: color 0.15s ease;
}

:deep(.el-collapse-item__header:hover) {
  color: #111827;
}

:deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

:deep(.el-collapse-item__content) {
  padding: 8px 0;
}

/* 搜索框样式优化 */
:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  transition: all 0.15s ease;
}

:deep(.el-input__wrapper:hover) {
  border-color: #d1d5db;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #9ca3af;
  box-shadow: 0 0 0 3px rgba(156, 163, 175, 0.1);
}

:deep(.el-input__inner) {
  font-size: 13px;
  color: #111827;
}

:deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

/* 滚动条样式 - 细腻的OpenAI风格 */
.palette-content::-webkit-scrollbar {
  width: 4px;
}

.palette-content::-webkit-scrollbar-track {
  background: transparent;
}

.palette-content::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

.palette-content::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
