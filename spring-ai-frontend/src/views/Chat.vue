<template>
  <McLayout class="chat-container">
    <!-- Header -->
    <McHeader :title="'AI 智能助手'" :logoImg="'https://matechat.gitcode.com/logo.svg'">
      <template #operationArea>
        <div class="operations">
          <el-tag v-if="isConnected" type="success" size="small">连接中</el-tag>
          <el-button 
            v-if="isConnected" 
            type="danger" 
            size="small" 
            @click="disconnect"
            style="margin-left: 8px"
          >
            断开连接
          </el-button>
        </div>
      </template>
    </McHeader>

    <!-- 欢迎页 -->
    <McLayoutContent
      v-if="showWelcome && messages.length === 0"
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
      <template v-for="(msg, idx) in messages" :key="idx">
        <!-- 用户消息 -->
        <McBubble
          v-if="msg.from === 'user'"
          :content="msg.content"
          :align="'right'"
          :avatarConfig="{ imgSrc: userAvatar }"
        />
        <!-- AI 消息（支持富文本） -->
        <div v-else class="ai-message-wrapper">
          <div class="ai-avatar">
            <img src="https://matechat.gitcode.com/logo.svg" alt="AI" />
          </div>
          <div class="ai-content">
            <div v-if="msg.loading" class="loading-indicator">
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
            </div>
            <RichTextContent v-else :content="msg.content" />
          </div>
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
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ChatAPI } from '@/api/chat'
import RichTextContent from '@/components/RichTextContent.vue'

const authStore = useAuthStore()

// 用户头像
const userAvatar = computed(() => authStore.userInfo?.userAvatar || 'https://matechat.gitcode.com/png/demo/userAvatar.svg')

// 欢迎描述
const description = [
  'AI 智能助手可以帮助您解答问题、生成内容、处理任务等。',
  '作为 AI 模型，提供的答案可能不总是准确的，您的反馈可以帮助我们做得更好。',
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
      label: '帮我生成一张图片',
      iconConfig: { name: 'icon-info-o', color: 'rgb(255, 215, 0)' },
      desc: '测试图片生成功能',
    },
    {
      value: 'music',
      label: '创作一段音乐',
      iconConfig: { name: 'icon-priority', color: '#3ac295' },
      desc: '测试音乐生成功能',
    },
  ],
}

// 响应式数据
const showWelcome = ref(true)
const inputValue = ref('')
const isConnected = ref(false)
const messages = ref<any[]>([])
let currentAbortController: AbortController | null = null

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
  messages.value.push({
    from: 'user',
    content: content,
  })

  // 创建 AI 消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    from: 'model',
    content: '',
    loading: true,
  })

  // 连接 SSE 并流式接收
  fetchStreamData(content, aiMessageIndex)
}

// 流式获取数据
const fetchStreamData = async (userMessage: string, aiMessageIndex: number) => {
  try {
    const abortController = ChatAPI.streamChat(userMessage, {
      onMessage: (data) => {
        console.log('💬 Chat.vue 收到消息:', {
          role: data.role,
          content: data.content,
          toolCallsCount: data.toolCalls?.length || 0,
          aiMessageIndex: aiMessageIndex,
          currentContent: messages.value[aiMessageIndex]?.content || '',
          currentContentLength: messages.value[aiMessageIndex]?.content?.length || 0
        })
        
        // 关闭加载状态
        messages.value[aiMessageIndex].loading = false
        
        // 累积内容
        const oldContent = messages.value[aiMessageIndex].content
        messages.value[aiMessageIndex].content += data.content || ''
        
        console.log('✏️ 内容更新:', {
          before: oldContent,
          after: messages.value[aiMessageIndex].content,
          added: data.content,
          beforeLength: oldContent.length,
          afterLength: messages.value[aiMessageIndex].content.length
        })
        
        // 如果有工具调用，添加观察信息
        if (data.toolCalls && data.toolCalls.length > 0) {
          const observations = data.toolCalls.map((toolCall: any) => {
            // 从toolCall中提取function信息
            const func = toolCall.function || {}
            const toolName = func.name || toolCall.name || '未知工具'
            const toolOutput = func.output || toolCall.output || ''
            const toolArguments = func.arguments || toolCall.arguments || ''
            
            // 格式化工具结果
            let resultText = toolOutput
            if (!resultText && toolArguments) {
              // 如果没有output，显示参数信息
              try {
                const args = JSON.parse(toolArguments)
                resultText = Object.entries(args).map(([k, v]) => `${k}: ${v}`).join(', ')
              } catch {
                resultText = toolArguments
              }
            }
            
            // 如果结果是对象，格式化输出
            if (typeof resultText === 'object') {
              resultText = JSON.stringify(resultText, null, 2)
            }
            
            // 构建观察信息
            if (toolOutput) {
              return `**观察：**${toolName}工具已经完成，结果为：${resultText}`
            } else {
              return `**观察：**${toolName}工具正在执行中...`
            }
          }).join('\n\n')
          
          // 在内容后面添加观察信息（用两个换行分隔）
          if (observations) {
            messages.value[aiMessageIndex].content += '\n' + observations + '\n'
          }
          
          console.log('🛠️ 添加工具调用观察信息:', observations)
        }
        
        console.log('📊 当前消息数组:', messages.value)
      },
      onError: (error) => {
        console.error('SSE 错误:', error)
        messages.value[aiMessageIndex].loading = false
        messages.value[aiMessageIndex].content = '抱歉，连接失败: ' + error.message
        isConnected.value = false
        currentAbortController = null
        ElMessage.error('连接失败: ' + error.message)
      },
      onComplete: () => {
        console.log('✅ SSE 连接完成')
        messages.value[aiMessageIndex].loading = false
        isConnected.value = false
        currentAbortController = null
      }
    })

    currentAbortController = abortController
    isConnected.value = true
    
  } catch (error: any) {
    console.error('连接 SSE 失败:', error)
    messages.value[aiMessageIndex].loading = false
    messages.value[aiMessageIndex].content = '抱歉，发送失败，请重试。'
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
.chat-container {
  width: 100%;
  height: 100vh;
  margin: 0 auto;
  padding: 20px;
  gap: 8px;
  background: #fff;
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

.ai-content {
  flex: 1;
  background: #f7f8fa;
  border-radius: 12px;
  padding: 12px 16px;
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
