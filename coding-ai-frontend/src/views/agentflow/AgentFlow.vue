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
        <el-button class="icon-btn" text circle @click="clearCanvas" title="Clear Canvas">
          <el-icon><Delete /></el-icon>
        </el-button>
        <el-button class="icon-btn" text circle @click="saveGraph" title="Save">
          <el-icon><FolderChecked /></el-icon>
        </el-button>
        <el-button class="icon-btn" text circle>
          <el-icon><Setting /></el-icon>
        </el-button>
      </div>
    </header>

    <!-- 画布区域 -->
    <div class="flow-wrapper">
      <!-- 节点选择侧边栏 -->
      <div v-show="showNodePalette" class="node-palette-sidebar">
        <NodePalette />
      </div>

      <!-- 左上角悬浮工具栏 -->
      <div class="floating-toolbar">
        <el-button type="primary" circle class="add-node-btn" @click="showNodePalette = !showNodePalette">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <!-- 右上角操作区 -->
      <div class="floating-actions">
        <el-button circle class="action-btn" color="#6200ee" style="color: white" @click="toggleChat">
          <el-icon><ChatDotRound /></el-icon>
        </el-button>
      </div>

      <VueFlow
        v-model="elements"
        :default-viewport="{ zoom: 1 }"
        :min-zoom="0.2"
        :max-zoom="4"
        class="agent-flow"
        @node-click="onNodeClick"
        @pane-click="onPaneClick"
        @dragover="onDragOver"
        @drop="onDrop"
        @edge-context-menu="onEdgeContextMenu"
        @node-context-menu="onNodeContextMenu"
        style="background-color: rgb(25, 27, 31)"
      >
        <Background :variant="BackgroundVariant.Dots" :gap="20" :size="2" pattern-color="#666666" />
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
              <!-- 状态图标 -->
              <div v-if="props.data.executionStatus" class="status-badge" :class="props.data.executionStatus">
                <el-icon v-if="props.data.executionStatus === 'running'" class="is-loading"><Loading /></el-icon>
                <el-icon v-else-if="props.data.executionStatus === 'success'"><CircleCheckFilled /></el-icon>
                <el-icon v-else-if="props.data.executionStatus === 'failed'"><CircleCloseFilled /></el-icon>
              </div>

              <div class="node-icon" :style="{ backgroundColor: props.data.color }">
                <el-icon :size="20" color="white">
                  <component :is="props.data.icon" />
                </el-icon>
              </div>
              <div class="node-info">
                <div class="node-label">{{ props.label }}</div>
                <div v-if="props.data.model || props.data.modelName" class="node-tag">
                  <span class="tag-icon">(-)</span> {{ props.data.modelName || props.data.model }}
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
                  :key="output.id"
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

      <!-- Context Menu -->
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      >
        <div class="menu-item delete" @click="deleteElement">
          <el-icon><Delete /></el-icon> 删除
        </div>
      </div>

      <!-- 聊天窗口 -->
      <div 
        v-show="chatVisible" 
        class="chat-window"
        ref="chatWindowRef"
        :style="{ 
          left: chatPosition.x + 'px', 
          top: chatPosition.y + 'px',
          bottom: 'auto',
          right: 'auto',
          transform: 'none'
        }"
      >
        <div class="chat-header" @mousedown="onChatDragStart">
          <span style="cursor: move; flex: 1;">Agent Chat</span>
          <el-icon class="close-icon" @click="chatVisible = false"><Close /></el-icon>
        </div>
        <div class="chat-messages" ref="messagesContainer">
          <div v-for="(msg, index) in chatMessages" :key="index" :class="['message-item', msg.role]">
            <div class="avatar">
              <el-icon v-if="msg.role === 'user'"><User /></el-icon>
              <el-icon v-else><Cpu /></el-icon>
            </div>
            <div class="message-content">
              <div v-if="msg.steps && msg.steps.length > 0" class="process-flow">
                <el-collapse>
                  <el-collapse-item title="Process Flow" name="1">
                    <div v-for="step in msg.steps" :key="step.nodeId" class="step-item">
                      <div class="step-icon" :style="{ backgroundColor: step.color }">
                        <el-icon :size="14" color="white"><component :is="step.icon" /></el-icon>
                      </div>
                      <span class="step-label">{{ step.label }}</span>
                      <el-icon class="step-status" :class="step.status">
                        <CircleCheckFilled v-if="step.status === 'success'" />
                        <CircleCloseFilled v-else />
                      </el-icon>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
              <div class="bubble" v-if="msg.content">{{ msg.content }}</div>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="1"
            placeholder="Type your question..."
            @keydown.enter.prevent="handleSendMessage"
            resize="none"
            class="input-area"
          />
          <el-button type="primary" circle @click="handleSendMessage" :loading="isSending" class="send-btn">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>

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

      <StartNodeDrawer
        v-model="startDrawerVisible"
        :node="selectedNode"
      />

      <DirectReplyDrawer
        v-model="directReplyDrawerVisible"
        :node="selectedNode"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VueFlow, Position, Handle, useVueFlow } from '@vue-flow/core'
import type { NodeMouseEvent, EdgeMouseEvent, Connection, Node, Edge } from '@vue-flow/core'
import { Background, BackgroundVariant } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'
import AgentNodeDrawer from '@/components/agentflow/AgentNodeDrawer.vue'
import ConditionAgentDrawer from '@/components/agentflow/ConditionAgentDrawer.vue'
import HumanInputDrawer from '@/components/agentflow/HumanInputDrawer.vue'
import ConditionNodeDrawer from '@/components/agentflow/ConditionNodeDrawer.vue'
import RetrieverDrawer from '@/components/agentflow/RetrieverDrawer.vue'
import ToolDrawer from '@/components/agentflow/ToolDrawer.vue'
import StartNodeDrawer from '@/components/agentflow/StartNodeDrawer.vue'
import DirectReplyDrawer from '@/components/agentflow/DirectReplyDrawer.vue'
import { AgentFlowAPI } from '@/api/agentFlow'
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
  FolderChecked,
  Tools,
  Connection as ConnectionIcon,
  Files,
  Delete,
  Close,
  Promotion,
  Loading,
  CircleCheckFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'

// 引入样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'

import NodePalette from '@/components/agentflow/NodePalette.vue'

const { onConnect, addEdges, removeEdges, removeNodes, project, toObject } = useVueFlow()
const route = useRoute()
const router = useRouter()

// 状态变量
const flowId = ref<number | string | null>(route.params.id ? String(route.params.id) : null)
const flowName = ref('Agent Flow Configuration')

// 聊天相关状态
const chatVisible = ref(false)
const chatMessages = ref<Array<{role: 'user' | 'assistant', content: string, type?: string, nodeId?: string, state?: any, steps?: any[]}>>([
  { role: 'assistant', content: '你好！有什么问题我可以帮助你吗？' }
])
const userInput = ref('')
const isSending = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// 抽屉状态
const drawerVisible = ref(false)
const showNodePalette = ref(false)
const conditionDrawerVisible = ref(false)
const humanDrawerVisible = ref(false)
const conditionNodeDrawerVisible = ref(false)
const retrieverDrawerVisible = ref(false)
const toolDrawerVisible = ref(false)
const startDrawerVisible = ref(false)
const directReplyDrawerVisible = ref(false)
const selectedNode = ref<any>(null)
const drawerType = ref('agent') // 'agent' | 'llm'

// Context Menu State
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  element: null as any,
  type: '' // 'node' or 'edge'
})

// Context Menu Handlers
const onNodeContextMenu = (event: NodeMouseEvent) => {
  event.event.preventDefault()
  showContextMenu(event.event, event.node, 'node')
}

const onEdgeContextMenu = (event: EdgeMouseEvent) => {
  event.event.preventDefault()
  showContextMenu(event.event, event.edge, 'edge')
}

const showContextMenu = (event: any, element: any, type: string) => {
  const container = document.querySelector('.flow-wrapper')
  const rect = container?.getBoundingClientRect()
  
  // Handle both MouseEvent and TouchEvent properties if needed, 
  // but contextmenu is primarily mouse.
  const clientX = event.clientX || (event.touches && event.touches[0]?.clientX) || 0
  const clientY = event.clientY || (event.touches && event.touches[0]?.clientY) || 0

  if (rect) {
      contextMenu.value = {
        visible: true,
        x: clientX - rect.left + 10, // Offset slightly
        y: clientY - rect.top + 5,
        element,
        type
      }
  }
}

const deleteElement = () => {
  if (contextMenu.value.type === 'node') {
    removeNodes([contextMenu.value.element])
  } else if (contextMenu.value.type === 'edge') {
    removeEdges([contextMenu.value.element])
  }
  contextMenu.value.visible = false
}

// 连线处理
onConnect((params: Connection) => {
  addEdges([params])
})

// 清空画布
const clearCanvas = () => {
  if (confirm('确定要清空画布吗？此操作无法撤销。')) {
    elements.value = []
  }
}

// 数据导出功能
const exportWorkflowData = () => {
  const flowData = toObject()
  
  // 转换为后端需要的格式
  const backendRequest = {
    name: 'Agent Flow Configuration',
    description: 'Created from Frontend',
    status: 1, // 启用
    nodes: flowData.nodes.map((node: Node) => {
      // 提取配置参数（根据节点类型过滤）
      const configParams: Record<string, any> = {}
      const data = node.data || {}
      
      // 优先使用 data.type，如果没有则尝试从 id 判断
      let type = data.type
      if (!type) {
        if (node.id === 'start' || node.id.startsWith('start')) {
          type = 'start'
        } else if (node.id.startsWith('llm')) {
          type = 'llm'
        } else {
          type = node.type // Fallback to 'custom' or actual type
        }
      }
      
      // Convert type to uppercase for backend enum alignment
      let backendType = type.toUpperCase()
      if (backendType === 'CONDITION-AGENT') backendType = 'CONDITION_AGENT'
      if (backendType === 'CONDITION-BASIC') backendType = 'CONDITION'
      if (backendType === 'HUMAN') backendType = 'HUMAN_INPUT'
      if (backendType === 'REPLY') backendType = 'END' // Assuming reply is end or generic
      if (backendType === 'CUSTOM' && (node.id === 'start' || node.id.startsWith('start'))) backendType = 'START'

      if (type === 'agent') {
        configParams.model = data.modelName
        configParams.chatModel = data.modelName
        configParams.temperature = data.temperature
        configParams.stream = data.streaming
        configParams.messages = data.messages
        // Map tools array of objects to list of strings
        if (data.tools && Array.isArray(data.tools)) {
            configParams.tools = data.tools.map((t: any) => t.name).filter(Boolean)
        } else {
            configParams.tools = []
        }
        configParams.enableMemory = data.enableMemory
        configParams.knowledgeBaseIds = data.knowledgeBaseIds
        configParams.topK = data.topK
        configParams.embeddingModel = data.embeddingModel
        configParams.hasKnowledge = data.hasKnowledge
      } else if (type === 'llm') {
        configParams.model = data.modelName
        configParams.temperature = data.temperature
        configParams.stream = data.streaming
        configParams.messages = data.messages
        configParams.enableMemory = data.enableMemory
        configParams.embeddingModel = data.embeddingModel
      } else if (type === 'condition-agent') {
        configParams.modelName = data.modelName
        configParams.input = data.input
        configParams.scenarios = data.scenarios
        configParams.sceneDescriptions = data.sceneDescriptions
      } else if (type === 'human') {
        configParams.inputPrompt = data.inputPrompt
        configParams.enableFeedback = data.enableFeedback
      } else if (type === 'condition-basic') {
        configParams.branches = data.branches
      } else if (type === 'retriever') {
        configParams.query = data.query
        configParams.topK = data.topK
        configParams.knowledgeBaseId = data.knowledgeBaseIds // Backend expects singular key for list?
        configParams.embeddingModel = data.embeddingModel
      } else if (type === 'tool') {
        configParams.toolName = data.tool
        configParams.toolParams = data.params
      } else if (type === 'reply' || type === 'end') {
        configParams.finalResult = data.message
      } else if (type === 'start') {
        if (data.flowState && Array.isArray(data.flowState)) {
            data.flowState.forEach((item: any) => {
                if (item.key) {
                    configParams[item.key] = item.value
                }
            })
        }
        configParams.persistState = data.persistState
      }

      return {
        id: node.id,
        label: node.label || data.label,
        type: backendType, 
        position: node.position,
        configParams: configParams,
        inputParams: [], 
        outputParams: []
      }
    }),
    edges: flowData.edges.map((edge: Edge) => ({
      id: edge.id,
      source: edge.source,
      sourceHandle: edge.sourceHandle,
      target: edge.target,
      targetHandle: edge.targetHandle
    }))
  }

  console.log('导出数据:', backendRequest)
  return backendRequest
}

// 保存工作流
const saveGraph = async () => {
  const flowData = exportWorkflowData()
  
  // 如果有 flowId，添加到请求中
  if (flowId.value) {
    (flowData as any).id = flowId.value
  }

  try {
    const res = await AgentFlowAPI.saveAgentFlow(flowData as any)
    if (res && res.data) {
      flowId.value = res.data
      ElMessage.success('保存成功')
      // 更新路由参数但不刷新页面
      router.replace({ params: { id: String(res.data) } })
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 聊天窗口拖拽逻辑
const chatWindowRef = ref<HTMLElement | null>(null)
const isChatDragging = ref(false)
const chatPosition = ref({ x: 0, y: 0 })
const chatStartPos = ref({ x: 0, y: 0 })
const chatOffset = ref({ x: 0, y: 0 })

// 初始化聊天窗口位置
const initChatPosition = () => {
  if (chatWindowRef.value) {
     // 默认位置：bottom: 20px, right: 80px
     // 我们需要将其转换为 top/left 或者 transform，这里使用 fixed 定位更简单
     // 为了支持拖拽，我们将 bottom/right 改为 fixed top/left 计算值
     const rect = chatWindowRef.value.getBoundingClientRect()
     chatPosition.value = {
        x: window.innerWidth - rect.width - 80, 
        y: window.innerHeight - rect.height - 20
     }
  }
}

const onChatDragStart = (e: MouseEvent) => {
  isChatDragging.value = true
  chatStartPos.value = { x: e.clientX, y: e.clientY }
  chatOffset.value = { ...chatPosition.value }
  
  document.addEventListener('mousemove', onChatDragMove)
  document.addEventListener('mouseup', onChatDragEnd)
}

const onChatDragMove = (e: MouseEvent) => {
  if (!isChatDragging.value) return
  
  const dx = e.clientX - chatStartPos.value.x
  const dy = e.clientY - chatStartPos.value.y
  
  chatPosition.value = {
    x: chatOffset.value.x + dx,
    y: chatOffset.value.y + dy
  }
}

const onChatDragEnd = () => {
  isChatDragging.value = false
  document.removeEventListener('mousemove', onChatDragMove)
  document.removeEventListener('mouseup', onChatDragEnd)
}

// 切换聊天窗口
const toggleChat = () => {
  chatVisible.value = !chatVisible.value
  if (chatVisible.value) {
    nextTick(() => {
      scrollToBottom()
      if (chatPosition.value.x === 0 && chatPosition.value.y === 0) {
         initChatPosition()
      }
    })
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!userInput.value.trim() || isSending.value) return
  
  if (!flowId.value) {
    ElMessage.warning('请先保存工作流')
    return
  }

  const prompt = userInput.value
  userInput.value = ''
  isSending.value = true

  // 添加用户消息
  chatMessages.value.push({ role: 'user', content: prompt })
  
  // 添加一个空的助手消息用于流式输出，带上 steps 数组
  const assistantMsgIndex = chatMessages.value.length
  chatMessages.value.push({ role: 'assistant', content: '', steps: [] })
  
  // 重置所有节点的执行状态
  elements.value.forEach((el) => {
    if (el.data) {
      el.data.executionStatus = undefined
    }
  })
  
  nextTick(scrollToBottom)

  try {
    const url = AgentFlowAPI.getExecuteStreamUrl(flowId.value, prompt)
    const eventSource = new EventSource(url)

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        
        if (data.type === 'chunk') {
          // 追加内容
          chatMessages.value[assistantMsgIndex].content += data.content
          scrollToBottom()
        } else if (data.type === 'node_complete') {
           // 处理节点完成状态
           const nodeId = data.nodeId
           const node = elements.value.find(n => n.id === nodeId)
           if (node) {
             node.data.executionStatus = 'success'
             
             // 添加到执行步骤
             if (!chatMessages.value[assistantMsgIndex].steps) {
                chatMessages.value[assistantMsgIndex].steps = []
             }
             chatMessages.value[assistantMsgIndex].steps.push({
               nodeId: node.id,
               label: node.label || node.data.label,
               icon: node.data.icon,
               color: node.data.color,
               status: 'success'
             })
             
             scrollToBottom()
           }
        } else if (data.type === 'node_start') {
           // 处理节点开始状态 (如果后端支持)
           const nodeId = data.nodeId
           const node = elements.value.find(n => n.id === nodeId)
           if (node) {
             node.data.executionStatus = 'running'
           }
        } else if (data.type === 'node_error') {
           // 处理节点错误状态
           const nodeId = data.nodeId
           const node = elements.value.find(n => n.id === nodeId)
           if (node) {
             node.data.executionStatus = 'failed'
             
             if (!chatMessages.value[assistantMsgIndex].steps) {
                chatMessages.value[assistantMsgIndex].steps = []
             }
             chatMessages.value[assistantMsgIndex].steps.push({
               nodeId: node.id,
               label: node.label || node.data.label,
               icon: node.data.icon,
               color: node.data.color,
               status: 'failed'
             })
           }
        } else if (data.type === 'finish') {
          eventSource.close()
          isSending.value = false
        }
      } catch (e) {
        console.error('解析消息失败:', e)
      }
    }

    eventSource.onerror = (error) => {
      console.error('SSE错误:', error)
      eventSource.close()
      isSending.value = false
      // 如果消息为空，提示错误
      if (!chatMessages.value[assistantMsgIndex].content) {
        chatMessages.value[assistantMsgIndex].content = '执行出错，请重试。'
      }
    }
  } catch (error) {
    console.error('启动执行失败:', error)
    isSending.value = false
    chatMessages.value[assistantMsgIndex].content = '启动失败。'
  }
}

// 拖拽相关
const onDragOver = (event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const onDrop = (event: DragEvent) => {
  event.preventDefault()
  
  if (event.dataTransfer) {
    const nodeData = JSON.parse(event.dataTransfer.getData('application/json'))
    
    // 获取放置位置
    const flowWrapper = document.querySelector('.flow-wrapper')
    const { left, top } = flowWrapper?.getBoundingClientRect() || { left: 0, top: 0 }
    
    // 使用 project 函数转换坐标
    // 注意：project 已经处理了 zoom 和 pan
    // 我们需要传递相对于 vue-flow 容器的坐标
    const position = project({
      x: event.clientX - left,
      y: event.clientY - top,
    })

    // Prepare additional data based on node type
    const additionalData: any = { inputs: true, outputs: [{ id: 'out' }] }
    
    if (nodeData.type === 'condition-agent') {
      additionalData.outputs = [{ id: '0', label: '', labelClass: '' }, { id: '1', label: '', labelClass: '' }]
      additionalData.modelName = 'qwen3-max'
      additionalData.provider = 'BaiLian'
    } else if (nodeData.type === 'human') {
      additionalData.outputs = [
        { id: 'proceed', label: 'proceed', labelClass: 'text-blue' },
        { id: 'reject', label: 'reject', labelClass: 'text-red' }
      ]
      additionalData.inputPrompt = '请输入你的二次问题？'
    } else if (nodeData.type === 'condition-basic') {
      additionalData.outputs = [
        { id: 'if', label: 'IF', labelClass: 'text-blue' },
        { id: 'else', label: 'ELSE', labelClass: 'text-orange' }
      ]
    } else if (nodeData.type === 'agent' || nodeData.type === 'llm') {
       additionalData.modelName = 'qwen3-max'
       additionalData.provider = 'BaiLian'
       additionalData.embeddingModel = 'text-embedding-v4'
    } else if (nodeData.type === 'retriever') {
       additionalData.embeddingModel = 'text-embedding-v4'
    } else if (nodeData.type === 'start') {
      additionalData.inputs = false
    }

    const newNode = {
      id: `${nodeData.type}-${Date.now()}`,
      type: 'custom',
      label: nodeData.label,
      position,
      data: { 
        ...nodeData.data, 
        label: nodeData.label,
        type: nodeData.type,
        ...additionalData
      }
    }

    elements.value.push(newNode)
  }
}

// 节点点击处理
const onPaneClick = () => {
  showNodePalette.value = false
  contextMenu.value.visible = false
}

const onNodeClick = (event: NodeMouseEvent) => {
  contextMenu.value.visible = false
  const { node } = event
  
  // 保存选中的节点
  if (node.data) {
    selectedNode.value = node
  }

  // Get node type from data or fallback to ID check
  const type = node.data?.type

  // Condition Agent 节点
  if (type === 'condition-agent' || node.id === 'condition-agent') {
    conditionDrawerVisible.value = true
    return
  }

  // Condition Basic 节点
  if (type === 'condition-basic' || node.id === 'condition-basic') {
    conditionNodeDrawerVisible.value = true
    return
  }

  // Human Input 节点
  if (type === 'human' || node.id === 'human') {
    humanDrawerVisible.value = true
    return
  }

  // Retriever 节点
  if (type === 'retriever' || node.id === 'retriever') {
    retrieverDrawerVisible.value = true
    return
  }

  // Tool 节点
  if (type === 'tool' || node.id === 'tool') {
    toolDrawerVisible.value = true
    return
  }

  // Start 节点
  if (type === 'start' || node.id === 'start' || node.id.startsWith('start')) {
    startDrawerVisible.value = true
    return
  }

  // Direct Reply 节点
  if (type === 'reply' || type === 'end' || node.id.startsWith('reply') || node.id.startsWith('end')) {
    directReplyDrawerVisible.value = true
    return
  }

  // 普通 Agent 节点
  if (type === 'agent' || type === 'llm' || (node.id && (node.id.startsWith('llm') || node.id.startsWith('agent'))) || (node.data && (node.data.modelName || node.data.model))) {
    // 判断是 LLM 还是 Agent
    if (type === 'llm' || (node.id && node.id.startsWith('llm'))) {
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
const elements = ref<(Node | Edge)[]>([])
</script>

<style scoped>
.agent-flow-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: rgb(25, 27, 31);
}

/* Header */
.flow-header {
  height: 60px;
  background: rgb(25, 27, 31);
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 10;
  color: white;
}

.header-right .el-button {
  color: black; /* Make header icons white */
  background-color: #ffffff;
}
.header-right .el-button:hover {
  transform: scale(1.1);
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
  color: white;
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

.node-palette-sidebar {
  position: absolute;
  top: 70px; /* Below the toolbar */
  left: 20px;
  width: 340px;
  height: calc(100% - 100px);
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 20;
  overflow: hidden;
}

.agent-flow {
  height: 100%;
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

.status-badge {
    position: absolute;
    top: -6px;
    left: -6px;
    background: white;
    border-radius: 50%;
    padding: 2px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    z-index: 20;
}
.status-badge .el-icon {
    font-size: 16px;
    display: block;
}
.status-badge.running { color: #409eff; }
.status-badge.success { color: #67c23a; }
.status-badge.failed { color: #f56c6c; }

.node-label {
  font-weight: 600;
  font-size: 14px;
  color: #374151;
}

.process-flow {
  width: 100%;
  margin-bottom: 8px;
}
.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.step-item:last-child {
  border-bottom: none;
}
.step-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-label {
  flex: 1;
  font-size: 13px;
  color: #606266;
}
.step-status {
  font-size: 16px;
}
.step-status.success {
  color: #67c23a;
}
.step-status.failed {
  color: #f56c6c;
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
  z-index: 10;
  pointer-events: auto;
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

/* Context Menu */
.context-menu {
  position: absolute;
  z-index: 100;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px;
  min-width: 120px;
  border: 1px solid #e5e7eb;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s;
}

.menu-item:hover {
  background-color: #f3f4f6;
}

.menu-item.delete {
  color: #ef4444;
}

.menu-item.delete:hover {
  background-color: #fee2e2;
}

/* Chat Window */
.chat-window {
  position: absolute;
  bottom: 20px;
  right: 80px; /* Left of the action button or adjust */
  width: 380px;
  height: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 50;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.chat-header {
  height: 50px;
  background: #6200ee;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  font-weight: 600;
}

.close-icon {
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
}
.close-icon:hover {
  background: rgba(255, 255, 255, 0.2);
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f9fafb;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 8px;
  max-width: 85%;
}

.message-item.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.message-item.assistant {
  align-self: flex-start;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #6b7280;
}

.message-item.user .avatar {
  background: #dbeafe;
  color: #3b82f6;
}

.message-item.assistant .avatar {
  background: #ede9fe;
  color: #6200ee;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-item.user .bubble {
  background: #3b82f6;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .bubble {
  background: white;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 4px;
}

.chat-input {
  padding: 12px;
  background: white;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-area {
  flex: 1;
}

.send-btn {
  background-color: #6200ee;
  border-color: #6200ee;
}
.send-btn:hover {
  background-color: #5b00dc;
  border-color: #5b00dc;
}


</style>
