<template>
  <div class="agent-flow-container">
    <!-- 顶部导航栏 -->
    <header class="flow-header">
      <div class="header-left">
        <div class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="flow-title">
          <span>测试</span>
          <el-icon class="edit-icon"><EditPen /></el-icon>
        </div>
      </div>
      <div class="header-right">
        <el-button class="icon-btn" text circle>
          <el-icon><img src="https://api.iconify.design/ph:code.svg" alt="code" /></el-icon>
        </el-button>
        <el-button class="icon-btn" text circle>
          <el-icon><img src="https://api.iconify.design/ph:floppy-disk.svg" alt="save" /></el-icon>
        </el-button>
        <el-button class="icon-btn" text circle>
          <el-icon><Setting /></el-icon>
        </el-button>
      </div>
    </header>

    <!-- 画布区域 -->
    <div class="flow-wrapper">
      <!-- 左上角悬浮工具栏 -->
      <div class="floating-toolbar">
        <el-button type="primary" circle class="add-node-btn">
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button type="danger" circle class="ai-btn">
          <el-icon><MagicStick /></el-icon>
        </el-button>
      </div>

      <!-- 右上角操作区 -->
      <div class="floating-actions">
        <el-button circle class="action-btn" type="success">
          <el-icon><Document /></el-icon>
        </el-button>
        <el-button circle class="action-btn" color="#6200ee" style="color: white">
          <el-icon><ChatDotRound /></el-icon>
        </el-button>
      </div>

      <VueFlow
        v-model="elements"
        :default-viewport="{ zoom: 1 }"
        :min-zoom="0.2"
        :max-zoom="4"
        fit-view-on-init
        class="agent-flow"
        @node-click="onNodeClick"
      >
        <Background :variant="BackgroundVariant.Dots" :gap="20" :size="1" pattern-color="#555" />
        <Controls />
        <MiniMap />

        <!-- 通用自定义节点模板 -->
        <template #node-custom="props">
          <div 
            :class="['custom-node-card', props.data.theme]"
            :style="{ minHeight: (props.data.outputs?.length || 0) * 40 + 'px' }"
          >
            <!-- 输入桩 -->
            <Handle
              v-if="props.data.inputs"
              type="target"
              :position="Position.Left"
              class="custom-handle input-handle"
            />

            <!-- 节点内容 -->
            <div class="node-body">
              <div class="node-icon" :style="{ backgroundColor: props.data.color }">
                <el-icon :size="20" color="white">
                  <component :is="props.data.icon" />
                </el-icon>
              </div>
              <div class="node-info">
                <div class="node-label">{{ props.label }}</div>
                <div v-if="props.data.model" class="node-tag">
                  <span class="tag-icon">(-)</span> {{ props.data.model }}
                </div>
              </div>
            </div>

            <!-- 底部装饰或额外图标 -->
            <div v-if="props.data.subIcon" class="node-sub-icon">
              <el-icon><component :is="props.data.subIcon" /></el-icon>
            </div>

            <!-- 输出桩逻辑 -->
            <template v-if="props.data.outputs">
              <!-- 单输出 -->
              <Handle
                v-if="props.data.outputs.length === 1"
                type="source"
                :position="Position.Right"
                class="custom-handle output-handle"
              />
              <!-- 多输出 (命名桩) -->
              <div v-else class="multi-handles">
                <div
                  v-for="(output, index) in props.data.outputs"
                  :key="index"
                  class="handle-wrapper"
                  :style="{ top: getHandlePosition(index, props.data.outputs.length) + '%' }"
                >
                  <span class="handle-label" :class="output.labelClass">{{ output.label }}</span>
                  <Handle
                    :id="output.id"
                    type="source"
                    :position="Position.Right"
                    class="custom-handle output-handle"
                    :style="{ top: '0', transform: 'translateY(0)' }"
                  />
                </div>
              </div>
            </template>
          </div>
        </template>
      </VueFlow>

      <!-- 侧边配置抽屉 -->
      <AgentNodeDrawer
        v-model="drawerVisible"
        :node="selectedNode"
        :node-type="drawerType"
      />

      <ConditionAgentDrawer
        v-model="conditionDrawerVisible"
        :node="selectedNode"
      />

      <HumanInputDrawer
        v-model="humanDrawerVisible"
        :node="selectedNode"
      />

      <ConditionNodeDrawer
        v-model="conditionNodeDrawerVisible"
        :node="selectedNode"
      />

      <RetrieverDrawer
        v-model="retrieverDrawerVisible"
        :node="selectedNode"
      />

      <ToolDrawer
        v-model="toolDrawerVisible"
        :node="selectedNode"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef } from 'vue'
import { VueFlow, useVueFlow, Position, Handle } from '@vue-flow/core'
import type { NodeMouseEvent } from '@vue-flow/core'
import { Background, BackgroundVariant } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import AgentNodeDrawer from '@/components/agentflow/AgentNodeDrawer.vue'
import ConditionAgentDrawer from '@/components/agentflow/ConditionAgentDrawer.vue'
import HumanInputDrawer from '@/components/agentflow/HumanInputDrawer.vue'
import ConditionNodeDrawer from '@/components/agentflow/ConditionNodeDrawer.vue'
import RetrieverDrawer from '@/components/agentflow/RetrieverDrawer.vue'
import ToolDrawer from '@/components/agentflow/ToolDrawer.vue'
import {
  EditPen,
  Setting,
  Plus,
  MagicStick,
  VideoPlay,
  Cpu,
  User,
  ChatDotRound,
  Share,
  ArrowLeft,
  Document,
  Tools,
  Connection,
  Files
} from '@element-plus/icons-vue'

// 引入样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'

// 抽屉状态
const drawerVisible = ref(false)
const conditionDrawerVisible = ref(false)
const humanDrawerVisible = ref(false)
const conditionNodeDrawerVisible = ref(false)
const retrieverDrawerVisible = ref(false)
const toolDrawerVisible = ref(false)
const selectedNode = ref(null)
const drawerType = ref('agent') // 'agent' | 'llm'

// 节点点击处理
const onNodeClick = (event: NodeMouseEvent) => {
  const { node } = event
  
  // 保存选中的节点
  if (node.data) {
    selectedNode.value = node
  }

  // Condition Agent 节点
  if (node.id === 'condition-agent') {
    conditionDrawerVisible.value = true
    return
  }

  // Condition Basic 节点
  if (node.id === 'condition-basic') {
    conditionNodeDrawerVisible.value = true
    return
  }

  // Human Input 节点
  if (node.id === 'human') {
    humanDrawerVisible.value = true
    return
  }

  // Retriever 节点
  if (node.id === 'retriever') {
    retrieverDrawerVisible.value = true
    return
  }

  // Tool 节点
  if (node.id === 'tool') {
    toolDrawerVisible.value = true
    return
  }

  // 普通 Agent 节点 (有 model 属性且不是 condition agent)
  if (node.data && node.data.model) {
    // 判断是 LLM 还是 Agent
    if (node.id.startsWith('llm')) {
      drawerType.value = 'llm'
    } else {
      drawerType.value = 'agent'
    }
    drawerVisible.value = true
  }
}

// 计算多输出桩的位置
const getHandlePosition = (index: number, total: number) => {
  if (total === 1) return 50
  // 简单分布：例如 2个桩分布在 30% 和 70%
  const step = 100 / (total + 1)
  return step * (index + 1)
}

// 初始节点数据 (模拟截图)
const elements = ref([
  // 1. Start Node
  {
    id: 'start',
    type: 'custom',
    label: 'Start',
    position: { x: 100, y: 100 },
    data: {
      theme: 'theme-green',
      color: '#4ade80', // Green
      icon: shallowRef(VideoPlay),
      inputs: false,
      outputs: [{ id: 'out' }]
    }
  },
  // 2. Condition Agent
  {
    id: 'condition-agent',
    type: 'custom',
    label: 'Condition Agent 0',
    position: { x: 150, y: 300 },
    data: {
      theme: 'theme-pink',
      color: '#fb7185', // Pink/Red
      icon: shallowRef(Share),
      model: 'qwen-plus',
      inputs: true,
      outputs: [
        { id: '0', label: '', labelClass: '' },
        { id: '1', label: '', labelClass: '' }
      ]
    }
  },
  // 3. Agent
  {
    id: 'agent',
    type: 'custom',
    label: 'Agent 0',
    position: { x: 450, y: 100 },
    data: {
      theme: 'theme-cyan',
      color: '#22d3ee', // Cyan
      icon: shallowRef(Cpu),
      model: 'qwen-max',
      subIcon: shallowRef(User), // 模拟那个小图标
      inputs: true,
      outputs: [{ id: 'out' }]
    }
  },
  // 4. LLM
  {
    id: 'llm',
    type: 'custom',
    label: 'LLM 0',
    position: { x: 450, y: 400 },
    data: {
      theme: 'theme-blue',
      color: '#60a5fa', // Blue
      icon: shallowRef(MagicStick),
      model: 'qwen-plus',
      inputs: true,
      outputs: [{ id: 'out' }]
    }
  },
  // 5. Human Input
  {
    id: 'human',
    type: 'custom',
    label: 'Human Input 0',
    position: { x: 450, y: 650 },
    data: {
      theme: 'theme-purple',
      color: '#818cf8', // Purple
      icon: shallowRef(User),
      inputs: true,
      outputs: [
        { id: 'proceed', label: 'proceed', labelClass: 'text-blue' },
        { id: 'reject', label: 'reject', labelClass: 'text-red' }
      ]
    }
  },
  // 6. Direct Reply
  {
    id: 'reply',
    type: 'custom',
    label: 'Direct Reply 0',
    position: { x: 800, y: 250 },
    data: {
      theme: 'theme-teal',
      color: '#2dd4bf', // Teal
      icon: shallowRef(ChatDotRound),
      inputs: true,
      outputs: [{ id: 'out' }]
    }
  },
  // 7. Condition (New)
  {
    id: 'condition-basic',
    type: 'custom',
    label: 'Condition 0',
    position: { x: 200, y: 600 },
    data: {
      theme: 'theme-orange',
      color: '#f59e0b', // Amber/Orange
      icon: shallowRef(Connection),
      inputs: true,
      outputs: [
        { id: 'if', label: 'IF', labelClass: 'text-blue' },
        { id: 'else', label: 'ELSE', labelClass: 'text-orange' }
      ]
    }
  },
  // 8. Retriever (New)
  {
    id: 'retriever',
    type: 'custom',
    label: 'Retriever 0',
    position: { x: 750, y: 650 },
    data: {
      theme: 'theme-slate',
      color: '#9ca3af', // Slate/GrayPurple
      icon: shallowRef(Files),
      inputs: true,
      outputs: [{ id: 'out' }]
    }
  },
  // 9. Tool (New)
  {
    id: 'tool',
    type: 'custom',
    label: 'Tool 0',
    position: { x: 850, y: 500 },
    data: {
      theme: 'theme-brown',
      color: '#d97706', // Brown/Amber-700
      icon: shallowRef(Tools),
      inputs: true,
      outputs: [{ id: 'out' }]
    }
  },
  // Edges
  { id: 'e1', source: 'start', target: 'condition-agent', animated: false, style: { stroke: '#f87171', strokeWidth: 2 } },
  { id: 'e2', source: 'condition-agent', target: 'agent', sourceHandle: '0', animated: false, style: { stroke: '#22d3ee', strokeWidth: 2 } },
  { id: 'e3', source: 'condition-agent', target: 'human', sourceHandle: '1', animated: false, style: { stroke: '#818cf8', strokeWidth: 2 } },
  { id: 'e4', source: 'agent', target: 'reply', animated: false, style: { stroke: '#2dd4bf', strokeWidth: 2 } },
  { id: 'e5', source: 'llm', target: 'reply', animated: false, style: { stroke: '#60a5fa', strokeWidth: 2 } },
  { id: 'e6', source: 'human', target: 'reply', sourceHandle: 'proceed', animated: false, style: { stroke: '#818cf8', strokeWidth: 2 } },
  { id: 'e7', source: 'human', target: 'llm', sourceHandle: 'reject', animated: false, style: { stroke: '#60a5fa', strokeWidth: 2 } },
])
</script>

<style scoped>
.agent-flow-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f9fafb;
}

/* Header */
.flow-header {
  height: 60px;
  background: white;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.flow-title {
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
}

.edit-icon {
  font-size: 16px;
  color: #999;
  cursor: pointer;
}

.flow-wrapper {
  flex: 1;
  position: relative;
}

/* Toolbar */
.floating-toolbar {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
  display: flex;
  gap: 12px;
}

.floating-actions {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  gap: 12px;
}

/* Custom Node Card Styles */
.custom-node-card {
  min-width: 180px;
  background: white;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid transparent;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  position: relative;
  transition: all 0.3s ease;
  
  /* Center content vertically */
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.custom-node-card:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.node-body {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.node-info {
  flex: 1;
}

.node-label {
  font-weight: 600;
  font-size: 14px;
  color: #374151;
}

.node-tag {
  font-size: 11px;
  color: #f97316; /* Orange tag color */
  background: #fff7ed;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 4px;
  display: inline-block;
  border: 1px solid #ffedd5;
}

/* Themes */
.theme-green { border-color: #bbf7d0; }
.theme-green .node-icon { background-color: #4ade80; }
.theme-green:hover { border-color: #4ade80; }

.theme-pink { border-color: #fecdd3; background-color: #fff1f2; }
.theme-pink .node-icon { background-color: #fb7185; }
.theme-pink:hover { border-color: #fb7185; }

.theme-cyan { border-color: #a5f3fc; background-color: #ecfeff; }
.theme-cyan .node-icon { background-color: #22d3ee; }
.theme-cyan:hover { border-color: #22d3ee; }

.theme-blue { border-color: #bfdbfe; background-color: #eff6ff; }
.theme-blue .node-icon { background-color: #60a5fa; }
.theme-blue:hover { border-color: #60a5fa; }

.theme-purple { border-color: #c7d2fe; background-color: #eef2ff; }
.theme-purple .node-icon { background-color: #818cf8; }
.theme-purple:hover { border-color: #818cf8; }

.theme-teal { border-color: #99f6e4; background-color: #f0fdfa; }
.theme-teal .node-icon { background-color: #2dd4bf; }
.theme-teal:hover { border-color: #2dd4bf; }

.theme-orange { border-color: #fcd34d; background-color: #fffbeb; }
.theme-orange .node-icon { background-color: #f59e0b; }
.theme-orange:hover { border-color: #f59e0b; }

.theme-brown { border-color: #fbbf24; background-color: #fff8e1; } /* Approximate brown/amber light */
.theme-brown .node-icon { background-color: #d97706; }
.theme-brown:hover { border-color: #d97706; }

.theme-slate { border-color: #e5e7eb; background-color: #f9fafb; }
.theme-slate .node-icon { background-color: #9ca3af; }
.theme-slate:hover { border-color: #9ca3af; }

/* Handles */
.custom-handle {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #9ca3af;
  border: 2px solid white;
}

.handle-wrapper {
  position: absolute;
  right: -6px; /* Adjust based on handle size */
  display: flex;
  align-items: center;
  transform: translateY(-50%);
}

.handle-label {
  position: absolute;
  left: 12px; /* Changed from right: 12px to left: 12px to push it OUTSIDE */
  font-size: 10px;
  white-space: nowrap;
  pointer-events: none;
}

.text-blue { color: #3b82f6; }
.text-red { color: #ef4444; }
.text-orange { color: #f97316; }

</style>
