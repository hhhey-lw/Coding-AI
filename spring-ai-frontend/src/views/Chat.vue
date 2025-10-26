<template>
  <div class="chat-page">
    <!-- 侧边栏 -->
    <ConversationSidebar
      :conversations="conversations"
      :activeConversationId="activeConversationId"
      @select-conversation="selectConversation"
      @create-conversation="createNewConversation"
      @toggle-collapse="onSidebarToggle"
    />

    <!-- 主聊天区域 -->
    <McLayout class="chat-container">
      <!-- Header -->
      <McHeader :title="'AI 智能助手'" :logoImg="'https://matechat.gitcode.com/logo.svg'">
        <template #operationArea>
          <div class="operations">
            <!-- 模型选择 -->
            <el-select v-model="selectedModel" placeholder="选择模型" size="small" style="width: 150px">
              <el-option label="React Agent" value="react" />
              <el-option label="Plan-Execute" value="plan-execute" />
            </el-select>
            
            <el-tag v-if="isConnected" type="success" size="small">连接中</el-tag>
            <el-button 
              v-if="isConnected" 
              type="danger" 
              size="small" 
              @click="disconnect"
            >
              断开连接
            </el-button>
          </div>
        </template>
      </McHeader>

      <!-- 欢迎页 -->
      <McLayoutContent
        v-if="showWelcome && messageBlocks.length === 0"
        style="display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px"
      >
        <McIntroduction
          :logoImg="'https://matechat.gitcode.com/logo2x.svg'"
          :title="'AI 智能助手'"
          :subTitle="'Hi，欢迎使用 AI 助手'"
          :description="description"
        />
        <McPrompt
          :list="introPrompt.list"
          :direction="introPrompt.direction"
          @itemClick="onPromptClick($event.label)"
        />
      </McLayoutContent>

      <!-- 消息列表 -->
      <McLayoutContent class="content-container" v-else>
        <template v-for="(block, idx) in messageBlocks" :key="idx">
          <!-- 用户消息 -->
          <McBubble
            v-if="block.type === 'user'"
            :content="block.content"
            :align="'right'"
            :avatarConfig="{ imgSrc: userAvatar }"
          />
          
          <!-- AI 消息块 -->
          <div v-else-if="block.type === 'assistant'" class="ai-message-wrapper">
            <div class="ai-avatar">
              <img src="https://matechat.gitcode.com/logo.svg" alt="AI" />
            </div>
            <div class="ai-content-wrapper">
              <!-- 加载状态 -->
              <div v-if="block.loading" class="loading-indicator">
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
              </div>
              
              <!-- 文本内容 -->
              <div v-else-if="block.content" class="ai-content">
                <RichTextContent :content="block.content" />
              </div>
            </div>
          </div>

          <!-- 计划卡片 -->
          <div v-else-if="block.type === 'plan'" class="plan-wrapper">
            <PlanCard
              :planData="block.planData"
              :currentStep="block.currentStep"
              :totalSteps="block.totalSteps"
              :percentage="block.percentage"
              :isFinished="block.isFinished"
            />
          </div>

          <!-- 工具调用卡片 -->
          <div v-else-if="block.type === 'tool'" class="tool-wrapper">
            <ToolCallCard :toolCall="block.toolCall" />
          </div>
        </template>
      </McLayoutContent>

      <!-- 输入区域 -->
      <McLayoutSender>
        <McInput 
          :value="inputValue" 
          :maxLength="2000" 
          :placeholder="'请输入您的问题...'"
          @change="(e: string) => (inputValue = e)" 
          @submit="onSubmit"
        >
          <template #extra>
            <div class="input-foot-wrapper">
              <div class="input-foot-left">
                <span class="input-foot-maxlength">{{ inputValue.length }}/2000</span>
              </div>
              <div class="input-foot-right">
                <el-button 
                  icon="Delete" 
                  size="small" 
                  :disabled="!inputValue" 
                  @click="inputValue = ''"
                >
                  清空
                </el-button>
              </div>
            </div>
          </template>
        </McInput>
      </McLayoutSender>
    </McLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ChatAPI, PlanExecuteEvent } from '@/api/chat'
import RichTextContent from '@/components/RichTextContent.vue'
import ConversationSidebar from '@/components/ConversationSidebar.vue'
import PlanCard from '@/components/PlanCard.vue'
import ToolCallCard from '@/components/ToolCallCard.vue'

const authStore = useAuthStore()

// 用户头像
const userAvatar = computed(() => authStore.userInfo?.userAvatar || 'https://matechat.gitcode.com/png/demo/userAvatar.svg')

// 欢迎描述
const description = [
  'AI 智能助手可以帮助您解答问题、生成内容、处理任务等。',
  '支持 React Agent 和 Plan-Execute 两种模式，满足不同的需求场景。',
]

// 欢迎提示词
const introPrompt = {
  direction: 'horizontal' as const,
  list: [
    {
      value: 'hello',
      label: '你好，介绍一下自己',
      iconConfig: { name: 'icon-star', color: '#5e7ce0' },
      desc: '了解 AI 助手的功能',
    },
    {
      value: 'generate',
      label: '帮我生成一张落日沙滩图片',
      iconConfig: { name: 'icon-info-o', color: 'rgb(255, 215, 0)' },
      desc: '测试图片生成功能',
    },
    {
      value: 'music',
      label: '帮我生成一幅落日沙滩图片，创作100字左右积极向上的歌词，然后生成一首歌曲',
      iconConfig: { name: 'icon-priority', color: '#3ac295' },
      desc: '测试 Plan-Execute 模式',
    },
  ],
}

// 消息块类型
interface MessageBlock {
  type: 'user' | 'assistant' | 'plan' | 'tool'
  content?: string
  loading?: boolean
  planData?: any
  currentStep?: number
  totalSteps?: number
  percentage?: number
  isFinished?: boolean
  toolCall?: any
}

// 响应式数据
const showWelcome = ref(true)
const inputValue = ref('')
const isConnected = ref(false)
const selectedModel = ref<'react' | 'plan-execute'>('react')
const messageBlocks = ref<MessageBlock[]>([])
const activeConversationId = ref('default')
const conversations = ref([
  { id: 'default', title: '新对话', time: new Date().toISOString() }
])
let currentAbortController: AbortController | null = null

// 计划管理 Map - 根据 planId 快速索引
const plansMap = ref<Map<string, number>>(new Map())

// 当前正在构建的文本块索引
let currentTextBlockIndex = -1

// 侧边栏折叠状态
const onSidebarToggle = (collapsed: boolean) => {
  console.log('侧边栏折叠状态:', collapsed)
}

// 选择对话
const selectConversation = (id: string) => {
  activeConversationId.value = id
  console.log('选择对话:', id)
}

// 创建新对话
const createNewConversation = () => {
  const newConv = {
    id: `conv_${Date.now()}`,
    title: '新对话',
    time: new Date().toISOString()
  }
  conversations.value.unshift(newConv)
  activeConversationId.value = newConv.id
  messageBlocks.value = []
  plansMap.value.clear()
  showWelcome.value = true
  currentTextBlockIndex = -1
  console.log('创建新对话:', newConv.id)
}

// 提示词点击
const onPromptClick = (label: string) => {
  inputValue.value = label
  onSubmit(label)
}

// 提交消息
const onSubmit = (text?: string) => {
  const content = text || inputValue.value
  if (!content.trim()) return

  // 隐藏欢迎页
  showWelcome.value = false
  
  // 清空输入
  inputValue.value = ''

  // 添加用户消息
  messageBlocks.value.push({
    type: 'user',
    content: content,
  })

  // 创建加载占位符
  messageBlocks.value.push({
    type: 'assistant',
    content: '',
    loading: true,
  })
  currentTextBlockIndex = messageBlocks.value.length - 1

  // 根据选择的模型调用不同的接口
  if (selectedModel.value === 'plan-execute') {
    fetchPlanExecuteData(content)
  } else {
    fetchStreamData(content)
  }
}

// React Agent 流式获取数据（使用统一的事件处理）
const fetchStreamData = async (userMessage: string) => {
  try {
    const abortController = ChatAPI.streamReactChat(userMessage, {
      onEvent: (event: PlanExecuteEvent) => {
        console.log('📬 收到 React 事件:', event)
        
        // 关闭加载状态
        if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
          messageBlocks.value[currentTextBlockIndex].loading = false
        }

        // 处理不同类型的事件
        switch (event.type) {
          case 'STEP_EXECUTION':
            // 流式文本内容
            if (event.content) {
              if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
                messageBlocks.value[currentTextBlockIndex].content += event.content
              }
            }
            break

          case 'TOOL_CALL':
            // 工具调用
            if (event.toolCalls && event.toolCalls.length > 0) {
              event.toolCalls.forEach(toolCall => {
                // 检查是否已添加
                const exists = messageBlocks.value.some(
                  (block) => block.type === 'tool' && block.toolCall?.id === toolCall.id
                )
                if (!exists) {
                  messageBlocks.value.push({
                    type: 'tool',
                    toolCall: toolCall
                  })
                  
                  // 创建新的文本块
                  messageBlocks.value.push({
                    type: 'assistant',
                    content: '',
                    loading: false,
                  })
                  currentTextBlockIndex = messageBlocks.value.length - 1
                }
              })
            }
            break

          case 'STREAM_END':
            // 流结束
            console.log('✅ React Agent 流结束')
            break

          default:
            console.log('未处理的事件类型:', event.type)
        }
      },
      onError: (error) => {
        console.error('React Agent SSE 错误:', error)
        if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
          messageBlocks.value[currentTextBlockIndex].loading = false
          messageBlocks.value[currentTextBlockIndex].content = '抱歉，连接失败: ' + error.message
        }
        isConnected.value = false
        currentAbortController = null
        ElMessage.error('连接失败: ' + error.message)
      },
      onComplete: () => {
        console.log('✅ React Agent SSE 连接完成')
        if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
          messageBlocks.value[currentTextBlockIndex].loading = false
        }
        isConnected.value = false
        currentAbortController = null
      }
    })

    currentAbortController = abortController
    isConnected.value = true
    
  } catch (error: any) {
    console.error('连接 React Agent SSE 失败:', error)
    if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
      messageBlocks.value[currentTextBlockIndex].loading = false
      messageBlocks.value[currentTextBlockIndex].content = '抱歉，发送失败，请重试。'
    }
    isConnected.value = false
    ElMessage.error('连接失败')
  }
}

// Plan-Execute Agent 流式获取数据
const fetchPlanExecuteData = async (userMessage: string) => {
  try {
    const abortController = ChatAPI.streamPlanExecuteChat(userMessage, {
      onEvent: (event: PlanExecuteEvent) => {
        console.log('📬 收到事件:', event)
        
        // 关闭加载状态
        if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
          messageBlocks.value[currentTextBlockIndex].loading = false
        }

        // 处理不同类型的事件
        switch (event.type) {
          case 'STEP_EXECUTION':
            // 流式文本内容
            if (event.content) {
              if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
                messageBlocks.value[currentTextBlockIndex].content += event.content
              }
            }
            break

          case 'TOOL_CALL':
            // 工具调用
            if (event.toolCalls && event.toolCalls.length > 0) {
              event.toolCalls.forEach(toolCall => {
                // 检查是否是 planning 工具
                if (toolCall.name === 'planning') {
                  try {
                    const args = JSON.parse(toolCall.arguments)
                    if (args.command === 'create') {
                      // 创建新的计划块
                      const planBlock: MessageBlock = {
                        type: 'plan',
                        planData: {
                          planId: '', // 等待 PLAN_PROGRESS 更新
                          title: args.title || '执行计划',
                          steps: args.steps || []
                        },
                        currentStep: 0,
                        totalSteps: args.steps?.length || 0,
                        percentage: 0,
                        isFinished: false
                      }
                      messageBlocks.value.push(planBlock)
                      
                      // 创建新的文本块
                      messageBlocks.value.push({
                        type: 'assistant',
                        content: '',
                        loading: false,
                      })
                      currentTextBlockIndex = messageBlocks.value.length - 1
                    }
                  } catch (e) {
                    console.error('解析 planning 参数失败:', e)
                  }
                } else {
                  // 其他工具调用，创建工具块
                  const exists = messageBlocks.value.some(
                    (block) => block.type === 'tool' && block.toolCall?.id === toolCall.id
                  )
                  if (!exists) {
                    messageBlocks.value.push({
                      type: 'tool',
                      toolCall: toolCall
                    })
                    
                    // 创建新的文本块
                    messageBlocks.value.push({
                      type: 'assistant',
                      content: '',
                      loading: false,
                    })
                    currentTextBlockIndex = messageBlocks.value.length - 1
                  }
                }
              })
            }
            break

          case 'PLAN_PROGRESS':
            // 更新计划进度
            if (event.planId) {
              // 查找对应的计划块
              let planBlockIndex = plansMap.value.get(event.planId)
              
              if (planBlockIndex === undefined) {
                // 首次接收到该计划ID，查找最近的计划块
                for (let i = messageBlocks.value.length - 1; i >= 0; i--) {
                  if (messageBlocks.value[i].type === 'plan' && !messageBlocks.value[i].planData?.planId) {
                    planBlockIndex = i
                    plansMap.value.set(event.planId, i)
                    break
                  }
                }
              }
              
              if (planBlockIndex !== undefined && messageBlocks.value[planBlockIndex]) {
                const planBlock = messageBlocks.value[planBlockIndex]
                if (planBlock.planData) {
                  planBlock.planData.planId = event.planId
                }
                planBlock.currentStep = event.currentStep || 0
                planBlock.totalSteps = event.totalSteps || 0
                planBlock.percentage = event.percentage || 0
                planBlock.isFinished = event.isFinished || false
              }
            }
            break

          case 'TOOL_RESULT':
          case 'STEP_COMPLETED':
            // 工具返回结果或步骤完成，累积到当前文本块
            const resultContent = event.result || event.output
            if (resultContent && currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
              messageBlocks.value[currentTextBlockIndex].content += `\n\n${resultContent}\n\n`
            }
            break

          case 'STREAM_END':
            // 流结束
            console.log('✅ Plan-Execute 流结束')
            break

          default:
            console.log('未处理的事件类型:', event.type)
        }
      },
      onError: (error) => {
        console.error('Plan-Execute SSE 错误:', error)
        if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
          messageBlocks.value[currentTextBlockIndex].loading = false
          messageBlocks.value[currentTextBlockIndex].content = '抱歉，连接失败: ' + error.message
        }
        isConnected.value = false
        currentAbortController = null
        ElMessage.error('连接失败: ' + error.message)
      },
      onComplete: () => {
        console.log('✅ Plan-Execute SSE 连接完成')
        if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
          messageBlocks.value[currentTextBlockIndex].loading = false
        }
        isConnected.value = false
        currentAbortController = null
      }
    })

    currentAbortController = abortController
    isConnected.value = true
    
  } catch (error: any) {
    console.error('连接 Plan-Execute SSE 失败:', error)
    if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
      messageBlocks.value[currentTextBlockIndex].loading = false
      messageBlocks.value[currentTextBlockIndex].content = '抱歉，发送失败，请重试。'
    }
    isConnected.value = false
    ElMessage.error('连接失败')
  }
}

// 断开连接
const disconnect = () => {
  if (currentAbortController) {
    currentAbortController.abort()
    currentAbortController = null
    isConnected.value = false
    ElMessage.info('已断开连接')
  }
}

// 组件卸载
onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  width: 100%;
  height: 100vh;
  background: #fff;
}

.chat-container {
  flex: 1;
  height: 100vh;
  padding: 20px;
  gap: 8px;
}

.operations {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.input-foot-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 8px;
}

.input-foot-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-foot-maxlength {
  font-size: 14px;
  color: #71757f;
}

.input-foot-right {
  display: flex;
  gap: 8px;
}

/* AI消息样式 */
.ai-message-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-content-wrapper {
  flex: 1;
  max-width: calc(100% - 60px);
}

.ai-content {
  background: #f7f8fa;
  border-radius: 12px;
  padding: 12px 16px;
}

/* 计划和工具包装器 */
.plan-wrapper,
.tool-wrapper {
  margin-left: 52px;
  max-width: calc(100% - 60px);
}

/* 加载动画 */
.loading-indicator {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: loading-bounce 1.4s infinite ease-in-out both;
}

.loading-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes loading-bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
