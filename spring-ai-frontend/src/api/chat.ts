/**
 * Chat API - 使用 Fetch + ReadableStream 处理 SSE 流式响应
 */

import { useAuthStore } from '@/stores/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// SSE 消息类型
export interface SSEMessage {
  role: 'user' | 'assistant'
  content: string
  toolCalls: any[]
}

// SSE 回调接口
export interface SSECallbacks {
  onMessage: (data: SSEMessage) => void
  onError: (error: Error) => void
  onComplete: () => void
}

/**
 * 流式聊天 - 使用 Fetch API + ReadableStream（安全，支持自定义请求头）
 */
function streamChat(message: string, callbacks: SSECallbacks): AbortController {
  const authStore = useAuthStore()
  const accessToken = authStore.getAccessToken()
  
  // 创建 AbortController 用于取消请求
  const abortController = new AbortController()

  // 构建 URL
  const url = `${API_BASE_URL}/ai/agent/react?prompt=${encodeURIComponent(message)}`

  console.log('🔗 开始 SSE 流式请求:', url)

  // 使用 fetch 发起请求
  fetch(url, {
    method: 'GET',
    headers: {
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache',
      ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {})
    },
    signal: abortController.signal
  })
  .then(async (response) => {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    if (!response.body) {
      throw new Error('Response body is null')
    }

    console.log('✅ SSE 连接已建立')

    // 获取 ReadableStream
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    
    let buffer = ''
    let messageCount = 0
    let chunkCount = 0
    let shouldStop = false

    // 读取流
    while (true && !shouldStop) {
      const { done, value } = await reader.read()

      if (done) {
        console.log('✅ SSE 流读取完成')
        callbacks.onComplete()
        break
      }

      chunkCount++
      
      // 打印原始块数据
      console.log(`🔷 [块 #${chunkCount}] 收到数据块:`, {
        byteLength: value.byteLength,
        arrayBuffer: value
      })

      // 解码数据
      const decodedChunk = decoder.decode(value, { stream: true })
      console.log(`🔷 [块 #${chunkCount}] 解码后的文本:`, decodedChunk)
      console.log(`🔷 [块 #${chunkCount}] 解码后的长度:`, decodedChunk.length)
      
      buffer += decodedChunk
      
      console.log(`📦 [块 #${chunkCount}] 当前缓冲区内容:`, buffer)
      console.log(`📦 [块 #${chunkCount}] 缓冲区长度:`, buffer.length)

      // 按行分割
      const lines = buffer.split('\n')
      console.log(`📋 [块 #${chunkCount}] 分割成 ${lines.length} 行`)
      
      // 保留最后一个不完整的行
      buffer = lines.pop() || ''
      console.log(`📋 [块 #${chunkCount}] 保留不完整行:`, buffer)

      // 处理每一行
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i]
        if (shouldStop) break
        
        console.log(`📝 [块 #${chunkCount}][行 ${i}] 处理行:`, line)
        
        // SSE 格式：data:{json} 或 data: {json}
        if (line.startsWith('data:')) {
          // 去掉 "data:" 前缀（可能有空格也可能没有）
          const jsonData = line.startsWith('data: ') 
            ? line.substring(6).trim()  // "data: " 有空格
            : line.substring(5).trim()  // "data:" 没空格
          console.log(`🎯 [块 #${chunkCount}][行 ${i}] 提取 JSON:`, jsonData)
          
          if (jsonData) {
            try {
              messageCount++
              
              // 解析 JSON
              const data = JSON.parse(jsonData)
              
              // 解析 toolCalls
              let parsedToolCalls = []
              if (typeof data.toolCalls === 'string') {
                try {
                  parsedToolCalls = JSON.parse(data.toolCalls)
                } catch (e) {
                  parsedToolCalls = []
                }
              } else {
                parsedToolCalls = data.toolCalls || []
              }
              data.toolCalls = parsedToolCalls

              // 检查是否是结束信号（使用特殊标记）
              const isEndSignal = data.role === 'system' && data.content === '[STREAM_END]'
              
              if (isEndSignal) {
                console.log(`✅ 收到结束信号 [STREAM_END]，共 ${messageCount} 条消息`)
                shouldStop = true
                break
              }

              // 所有消息都传递给回调（包括空 content 但有 toolCalls 的消息）
              console.log(`📬 [消息 #${messageCount}] 传递给回调:`, {
                role: data.role,
                content: data.content,
                contentLength: data.content?.length || 0,
                toolCallsCount: data.toolCalls.length
              })
              callbacks.onMessage(data)
              
            } catch (error) {
              console.error('❌ 解析消息失败:', jsonData, error)
            }
          }
        }
      }
    }
    
    // 正常完成
    console.log('✅ SSE 流处理完毕')
    callbacks.onComplete()
  })
  .catch((error) => {
    if (error.name === 'AbortError') {
      console.log('⚠️ SSE 请求已取消')
    } else {
      console.error('❌ SSE 请求失败:', error)
      callbacks.onError(error)
    }
  })

  return abortController
}

/**
 * 发送普通聊天消息（非流式）
 */
async function sendMessage(message: string): Promise<any> {
  const authStore = useAuthStore()
  const accessToken = authStore.getAccessToken()

  const response = await fetch(`${API_BASE_URL}/ai/agent/react`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {})
    },
    body: JSON.stringify({ message })
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  return response.json()
}

export const ChatAPI = {
  streamChat,
  sendMessage
}
