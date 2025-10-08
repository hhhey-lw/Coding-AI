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
.node-palette {
  width: 280px;
  height: 100%;
  background: white;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.palette-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.palette-header h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.palette-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.node-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.2s ease;
  background: white;
}

.node-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

.node-item:active {
  cursor: grabbing;
  transform: translateY(0);
}

.node-icon {
  margin-right: 12px;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #f0f9ff;
  border-radius: 6px;
}

.node-info {
  flex: 1;
  min-width: 0;
}

.node-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.node-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  word-break: break-all;
}

:deep(.el-collapse) {
  border: none;
}

:deep(.el-collapse-item__header) {
  height: 40px;
  line-height: 40px;
  background: #f5f7fa;
  border: none;
  font-weight: 500;
  color: #606266;
  padding: 0 12px;
}

:deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

:deep(.el-collapse-item__content) {
  padding: 8px 0;
}

/* 滚动条样式 */
.palette-content::-webkit-scrollbar {
  width: 6px;
}

.palette-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.palette-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.palette-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
