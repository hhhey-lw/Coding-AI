<template>
  <div class="chat-page">
    <!-- 侧边栏 -->
    <ConversationSidebar
      :conversations="conversations"
      :activeConversationId="activeConversationId"
      :loading="conversationLoading"
      :hasMore="hasMore"
      @select-conversation="selectConversation"
      @create-conversation="createNewConversation"
      @toggle-collapse="onSidebarToggle"
      @delete-conversation="deleteConversation"
      @load-more="loadMoreConversations"
      @refresh="refreshConversations"
    />

    <!-- 主聊天区域 -->
    <McLayout class="chat-container">
      <!-- Header -->
      <McHeader class="chat-mc-header" :title="'AI 智能助手'" :logoImg="'https://longcoding-ai-service.oss-cn-hangzhou.aliyuncs.com/files/a2507c9c79f749ac8f00c45d51192c23.png'">
        <template #operationArea>
          <div class="operations">
            <!-- 模型选择 -->
            <el-select v-model="selectedModel" placeholder="选择模型" size="small" class="model-select-box" style="width: 100px">
              <el-option label="React Agent" value="react" />
              <el-option label="Plan-Execute Agent" value="plan-execute" />
            </el-select>
          </div>
        </template>
      </McHeader>

      <!-- 欢迎页 -->
      <McLayoutContent
        v-if="showWelcome && messageBlocks.length === 0"
        style="display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px"
      >
        <McIntroduction
          class="chat-mc-introduction"
          :logoImg="'https://longcoding-ai-service.oss-cn-hangzhou.aliyuncs.com/files/a2507c9c79f749ac8f00c45d51192c23.png'"
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
      <McLayoutContent class="content-container" v-else ref="contentContainerRef">
        <template v-for="(block, idx) in messageBlocks" :key="idx">
          <!-- 用户消息 -->
          <McBubble
            v-if="block.type === 'user'"
            :content="block.content"
            :align="'right'"
            :avatarConfig="{ imgSrc: userAvatar }"
          />
          
          <!-- AI 消息块 -->
          <div v-else-if="block.type === 'assistant' && (block.loading || (block.content && block.content.trim()))" class="ai-message-wrapper">
            <div class="ai-avatar">
              <img src="https://longcoding-ai-service.oss-cn-hangzhou.aliyuncs.com/files/a2507c9c79f749ac8f00c45d51192c23.png" alt="AI" />
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
            <ToolCallCard :toolCall="block.toolCall" :toolResponse="block.toolResponse" />
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
        </McInput>
      </McLayoutSender>
    </McLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPause, Promotion } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ChatAPI, PlanExecuteEvent } from '@/api/chat'
import { ConversationAPI, ChatConversation } from '@/api/conversation'
import RichTextContent from '@/components/RichTextContent.vue'
import ConversationSidebar from '@/components/ConversationSidebar.vue'
import PlanCard from '@/components/PlanCard.vue'
import ToolCallCard from '@/components/ToolCallCard.vue'

const authStore = useAuthStore()
const router = useRouter()
const contentContainerRef = ref<HTMLElement | null>(null)

// 用户头像
const userAvatar = computed(() => authStore.userInfo?.userAvatar || 'https://matechat.gitcode.com/png/demo/userAvatar.svg')

// 欢迎描述
const description = [
  'AI 智能助手可以帮助您解答问题、联网搜索、生成图片、生成音乐等。',
]

// 欢迎提示词
const introPrompt = {
  direction: 'horizontal' as const,
  list: [
    {
      value: 'music',
      label: '帮我生成一幅落日沙滩图片。',
      iconConfig: { name: 'icon-priority', color: '#3ac295' }
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
  toolResponse?: any
}

// 响应式数据
const showWelcome = ref(true)
const inputValue = ref('')
const isConnected = ref(false)
const selectedModel = ref<'react' | 'plan-execute'>('react')
const messageBlocks = ref<MessageBlock[]>([])
const activeConversationId = ref<string>('')
const conversations = ref<ChatConversation[]>([])
const conversationLoading = ref(false)
const hasMore = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
let currentAbortController: AbortController | null = null

// 计划管理 Map - 根据 planId 快速索引
const plansMap = ref<Map<string, number>>(new Map())
// 计划定义缓存 - 存储 planId 对应的标题和步骤
const planDefinitions = ref<Map<string, { title: string, steps: string[] }>>(new Map())

// 当前正在构建的文本块索引
let currentTextBlockIndex = -1

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (contentContainerRef.value) {
    const container = contentContainerRef.value.$el || contentContainerRef.value
    container.scrollTop = container.scrollHeight
  }
}

// 监听消息变化自动滚动
watch(() => messageBlocks.value, () => {
  scrollToBottom()
}, { deep: true })

// 初始化：加载会话列表和设置模型
onMounted(async () => {
  // 从URL参数中获取模型类型
  const route = router.currentRoute.value
  const modelParam = route.query.model as string
  if (modelParam === 'react' || modelParam === 'plan-execute') {
    selectedModel.value = modelParam
    console.log('✅ 从URL参数设置模型:', modelParam)
  }
  
  await loadConversations()
})

// 加载会话列表
const loadConversations = async (append = false) => {
  try {
    conversationLoading.value = true
    const page = append ? currentPage.value : 1
    
    const response = await ConversationAPI.getConversationPage({
      pageNum: page,
      pageSize: pageSize.value,
      status: 'active'
    })
    
    if (response.code === 1 && response.data) {
      const pageData = response.data
      if (append) {
        conversations.value.push(...pageData.list)
      } else {
        conversations.value = pageData.list
      }
      
      currentPage.value = pageData.pageNum
      hasMore.value = pageData.list.length >= pageSize.value && conversations.value.length < pageData.total
      
      console.log('✅ 加载会话列表成功:', pageData)
    }
  } catch (error: any) {
    console.error('❌ 加载会话列表失败:', error)
    ElMessage.error(error.message || '加载会话列表失败')
  } finally {
    conversationLoading.value = false
  }
}

// 加载更多会话
const loadMoreConversations = async () => {
  if (conversationLoading.value || !hasMore.value) return
  currentPage.value++
  await loadConversations(true)
}

// 刷新会话列表
const refreshConversations = async () => {
  currentPage.value = 1
  await loadConversations(false)
}

// 侧边栏折叠状态
const onSidebarToggle = (collapsed: boolean) => {
  console.log('侧边栏折叠状态:', collapsed)
}

// 选择对话
const selectConversation = async (id: string) => {
  if (activeConversationId.value === id) return
  
  activeConversationId.value = id
  messageBlocks.value = []
  plansMap.value.clear()
  showWelcome.value = false
  currentTextBlockIndex = -1
  
  // 加载会话历史消息
  try {
    const response = await ConversationAPI.getConversationDetail(id)
    if (response.code === 1 && response.data && response.data.messages && response.data.messages.length > 0) {
      console.log('📜 加载历史消息:', response.data.messages.length, '条')
      
      // 将历史消息转换为 messageBlocks 显示
      const historyBlocks: MessageBlock[] = []
      // 用于根据 toolCallId 查找对应的 MessageBlock
      const toolCallMap = new Map<string, MessageBlock>()
      
      for (const msg of response.data.messages) {
        if (msg.role === 'USER') {
          // 用户消息
          historyBlocks.push({
            type: 'user',
            content: msg.content
          })
        } else if (msg.role === 'ASSISTANT') {
          // 助手消息
          if (msg.toolCalls && msg.toolCalls.length > 0) {
            // 如果有工具调用，创建工具调用块
            msg.toolCalls.forEach((toolCall: any) => {
              const block: MessageBlock = {
                type: 'tool',
                toolCall: {
                  id: toolCall.id,
                  name: toolCall.name,
                  arguments: toolCall.arguments
                }
              }
              historyBlocks.push(block)
              // 记录ID映射，以便后续关联响应
              toolCallMap.set(toolCall.id, block)
            })
          }
          
          // 添加助手文本内容（如果有）
          if (msg.content && msg.content.trim()) {
            historyBlocks.push({
              type: 'assistant',
              content: msg.content,
              loading: false
            })
          }
          
          // 检查是否是计划相关的消息 (Plan-Execute 模式)
          // 这里可能需要根据 plan 字段来恢复计划卡片
          // 但后端返回的历史消息中可能没有保留完整的 plan 状态，或者是以特殊文本形式
          // 暂时主要修复工具调用的显示
          
        } else if (msg.role === 'TOOL' || (msg.type === 'TOOL_RESPONSE' && msg.toolResponses)) {
          // 工具响应消息
          if (msg.toolResponses && msg.toolResponses.length > 0) {
            msg.toolResponses.forEach((response: any) => {
               const toolBlock = toolCallMap.get(response.id)
               if (toolBlock) {
                 toolBlock.toolResponse = {
                   id: response.id,
                   name: response.name,
                   responseData: response.responseData
                 }
               }
            })
          }
        }
      }
      
      messageBlocks.value = historyBlocks
      console.log('✅ 历史消息渲染完成，共', historyBlocks.length, '个消息块')
      scrollToBottom()
    }
  } catch (error: any) {
    console.warn('⚠️ 加载历史消息失败:', error)
  }
  
  console.log('选择对话:', id)
}

// 创建新对话
const createNewConversation = async () => {
  try {
    const response = await ConversationAPI.createConversation({
      title: '新对话'
    })
    
    if (response.code === 1 && response.data) {
      const conversationId = response.data
      console.log('✅ 创建新对话成功:', conversationId)
      
      // 刷新会话列表
      await refreshConversations()
      
      // 切换到新会话
      activeConversationId.value = conversationId
      messageBlocks.value = []
      plansMap.value.clear()
      showWelcome.value = true
      currentTextBlockIndex = -1
      
      ElMessage.success('创建新对话成功')
    }
  } catch (error: any) {
    console.error('❌ 创建新对话失败:', error)
    ElMessage.error(error.message || '创建新对话失败')
  }
}

// 删除会话
const deleteConversation = async (id: string) => {
  try {
    const response = await ConversationAPI.deleteConversation(id)
    
    if (response.code === 1) {
      console.log('✅ 删除会话成功:', id)
      ElMessage.success('删除会话成功')
      
      // 从列表中移除
      conversations.value = conversations.value.filter(conv => conv.id !== id)
      
      // 如果删除的是当前会话，切换到第一个会话或创建新会话
      if (activeConversationId.value === id) {
        if (conversations.value.length > 0) {
          activeConversationId.value = conversations.value[0].id
        } else {
          await createNewConversation()
        }
      }
    }
  } catch (error: any) {
    console.error('❌ 删除会话失败:', error)
    ElMessage.error(error.message || '删除会话失败')
  }
}

// 提示词点击
const onPromptClick = (label: string) => {
  inputValue.value = label
  onSubmit(label)
}

// 提交消息
const onSubmit = async (text?: string) => {
  const content = text || inputValue.value
  if (!content.trim()) return

  // 🔑 关键：如果没有会话ID，先创建会话
  if (!activeConversationId.value) {
    console.log('⚠️ 没有会话ID，先创建新会话...')
    try {
      const response = await ConversationAPI.createConversation({
        title: content.substring(0, 20) + (content.length > 20 ? '...' : '') // 使用消息前20字符作为标题
      })
      
      if (response.code === 1 && response.data) {
        activeConversationId.value = response.data
        console.log('✅ 创建新会话成功，会话ID:', activeConversationId.value)
        
        // 刷新会话列表
        await refreshConversations()
      } else {
        ElMessage.error('创建会话失败')
        return
      }
    } catch (error: any) {
      console.error('❌ 创建会话失败:', error)
      ElMessage.error(error.message || '创建会话失败')
      return
    }
  }

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
  console.log('📤 发送消息到会话:', activeConversationId.value)
  if (selectedModel.value === 'plan-execute') {
    fetchPlanExecuteData(content)
  } else {
    fetchStreamData(content)
  }
}

// React Agent 流式获取数据（使用统一的事件处理）
const fetchStreamData = async (userMessage: string) => {
  try {
    const abortController = ChatAPI.streamReactChat(
      userMessage, 
      {
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
              event.toolCalls.forEach((toolCall: any) => {
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
            
          case 'TOOL_RESPONSE':
            // 工具响应
            if (event.toolResponses && event.toolResponses.length > 0) {
              event.toolResponses.forEach((response: any) => {
                // 查找对应的工具调用块
                const block = messageBlocks.value.find(
                  (b) => b.type === 'tool' && b.toolCall?.id === response.id
                )
                if (block) {
                  block.toolResponse = response
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
    },
    activeConversationId.value // 传递会话ID
    )

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
    const abortController = ChatAPI.streamPlanExecuteChat(
      userMessage,
      {
      onEvent: (event: PlanExecuteEvent) => {
        // console.log('📬 收到事件:', event)
        
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
              event.toolCalls.forEach((toolCall: any) => {
                // 检查是否是 planning 工具
                if (toolCall.name === 'planning') {
                  try {
                    const args = JSON.parse(toolCall.arguments)
                    if (args.command === 'create') {
                      // 创建新的工具调用块
                      messageBlocks.value.push({
                        type: 'tool',
                        toolCall: toolCall
                      })
                      
                      // 注意：这里不再立即创建 plan 块，而是等待 PLAN_PROGRESS 事件
                      // 但我们需要保存计划的定义(步骤和标题)，以便后续使用
                      // 由于此时还没有 planId，我们暂时无法存入 planDefinitions Map
                      // 我们会在 TOOL_RESPONSE 中获取 planId 并关联存储
                      
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

          case 'TOOL_RESPONSE':
             // 工具响应
             if (event.toolResponses && event.toolResponses.length > 0) {
               event.toolResponses.forEach((response: any) => {
                 // 查找对应的工具调用块
                 const block = messageBlocks.value.find(
                   (b) => b.type === 'tool' && b.toolCall?.id === response.id
                 )
                 if (block) {
                   block.toolResponse = response

                   // 如果是 planning 工具的响应，更新对应的计划块ID
                   if (block.toolCall?.name === 'planning' && response.responseData) {
                     try {
                       const data = JSON.parse(response.responseData)
                       if (data.planId) {
                         // 尝试解析请求参数以获取标题和步骤
                         if (block.toolCall?.arguments) {
                           try {
                             const args = JSON.parse(block.toolCall.arguments)
                             if (args.steps) {
                               // 存储计划定义
                               planDefinitions.value.set(data.planId, {
                                 title: args.title || '执行计划',
                                 steps: args.steps
                               })
                             }
                           } catch (e) {
                             console.error('解析 planning 请求参数失败:', e)
                           }
                         }
                       }
                     } catch (e) {
                       // ignore parse error
                     }
                   }
                 }
               })
             }
             // 如果有内容，也作为文本显示（兼容 Step Execution 结果）
             if (event.content) {
                if (currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
                  const incoming = String(event.content).trim()
                  if (incoming) {
                    const existing = String(messageBlocks.value[currentTextBlockIndex].content || '').trim()
                    if (!existing) {
                      messageBlocks.value[currentTextBlockIndex].content = incoming
                    } else if (!existing.includes(incoming)) {
                      messageBlocks.value[currentTextBlockIndex].content += `\n\n${incoming}\n\n`
                    }
                  }
                }
             }
             break

          case 'PLAN_PROGRESS':
            // 收到 PLAN_PROGRESS 时，创建一个新的计划块，显示最新的进度
            // 只在有新进度时才显示（即不是第一次创建计划时）
            if (event.plan) {
              const planData = event.plan
              
              // 检查是否重复（与最近的一个该ID的计划块比较）
              // 防止结束时产生重复的计划块
              let lastSameIdBlock = null
              for (let i = messageBlocks.value.length - 1; i >= 0; i--) {
                const block = messageBlocks.value[i]
                if (block.type === 'plan' && block.planData?.planId === planData.planId) {
                  lastSameIdBlock = block
                  break
                }
              }
              
              if (lastSameIdBlock) {
                // 如果状态完全一致，则不创建新块
                if (lastSameIdBlock.currentStep === (planData.currentStep || 0) &&
                    lastSameIdBlock.percentage === (planData.percentage || 0) &&
                    lastSameIdBlock.isFinished === (planData.isFinished || false)) {
                  break
                }
              }
              
              // 查找原始计划的标题和步骤
              let originalPlanTitle = '执行计划'
              let originalPlanSteps: string[] = []
              
              // 1. 尝试从 planDefinitions 中获取
              const definition = planDefinitions.value.get(planData.planId)
              if (definition) {
                originalPlanTitle = definition.title
                originalPlanSteps = definition.steps
              } else {
                // 2. 如果没有定义，尝试从 messageBlocks 中查找历史计划块
                for (let i = messageBlocks.value.length - 1; i >= 0; i--) {
                  const block = messageBlocks.value[i]
                  if (block.type === 'plan' && block.planData) {
                     if (block.planData.planId === planData.planId) {
                       originalPlanTitle = block.planData.title
                       originalPlanSteps = block.planData.steps
                       break
                     }
                  }
                }
              }
              
              // 创建新的计划块（复制一份）
              const newPlanBlock: MessageBlock = {
                type: 'plan',
                planData: {
                  planId: planData.planId,
                  title: originalPlanTitle,
                  steps: originalPlanSteps
                },
                currentStep: planData.currentStep || 0,
                totalSteps: planData.totalSteps || 0,
                percentage: planData.percentage || 0,
                isFinished: planData.isFinished || false
              }
              
              messageBlocks.value.push(newPlanBlock)
              
              // 更新映射，指向最新的这个块
              plansMap.value.set(planData.planId, messageBlocks.value.length - 1)
              
              // 创建新的文本块，确保后续的文本输出显示在计划卡片下方
              messageBlocks.value.push({
                type: 'assistant',
                content: '',
                loading: true
              })
              currentTextBlockIndex = messageBlocks.value.length - 1
            }
            break

          case 'STEP_COMPLETE':
          case 'STEP_COMPLETED':
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
                  if (event.steps && Array.isArray(event.steps)) {
                     planBlock.planData.steps = event.steps
                  }
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
            // 工具返回结果或步骤完成
            const resultContent = event.result || event.output
            
            // 尝试解析并更新工具响应
            if (event.toolCallId) {
               // 查找对应的工具调用块
               const block = messageBlocks.value.find(
                 (b) => b.type === 'tool' && b.toolCall?.id === event.toolCallId
               )
               if (block) {
                 block.toolResponse = {
                   id: event.toolCallId,
                   name: block.toolCall?.name || 'unknown',
                   responseData: resultContent || '{}'
                 }
               }
            }
            
            // 累积到当前文本块
            if (resultContent && currentTextBlockIndex >= 0 && messageBlocks.value[currentTextBlockIndex]) {
              // 检查是否包含媒体链接，如果包含则不添加到文本中，因为已经在工具卡片中展示了
              const hasMedia = resultContent.includes('"imageUrl"') || resultContent.includes('"musicUrl"')
              if (!hasMedia) {
                messageBlocks.value[currentTextBlockIndex].content += `\n\n${resultContent}\n\n`
              }
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
    },
    activeConversationId.value // 传递会话ID
    )

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
  box-sizing: border-box;
  overflow: hidden; /* 防止整体溢出 */
}

.operations {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.chat-mc-header img) {
  width: 24px !important;
  height: 24px !important;
  object-fit: contain;
}

:deep(.chat-mc-introduction img) {
  width: 56px !important;
  height: 56px !important;
  object-fit: contain;
}

.content-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto; /* 仅纵向滚动 */
  overflow-x: hidden; /* 防止横向溢出 */
  padding-right: 10px; /* 给滚动条留出空间 */
  flex: 1; /* 占据剩余高度 */
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
  /* background: #f7f8fa; 移除背景，交给内部元素控制 */
  /* border-radius: 12px; */
  /* padding: 12px 16px; */
  width: 100%;
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
