<template>
  <div class="tool-call-card">
    <div class="tool-call-header" @click="toggleCollapse">
      <div class="tool-icon">
        <el-icon :size="16"><Tools /></el-icon>
      </div>
      <span class="tool-label">工具调用</span>
      <el-tag size="small" type="info" effect="plain">{{ toolCall.name }}</el-tag>
      <span v-if="isCollapsed && toolResponse" class="tool-status tool-status-success">
        <el-icon color="#67c23a" style="margin-right: 4px; vertical-align: middle;"><Select /></el-icon>
        已执行完毕
      </span>
      <span v-else-if="!toolResponse" class="tool-status tool-status-loading">
        <el-icon class="is-loading" color="#409eff" style="margin-right: 4px; vertical-align: middle;"><Loading /></el-icon>
        执行中...
      </span>
      <el-icon class="collapse-icon" :class="{ 'is-collapsed': isCollapsed }">
        <ArrowRight />
      </el-icon>
    </div>

    <el-collapse-transition>
      <div class="tool-call-body" v-show="!isCollapsed">
        <!-- Tab 切换 -->
      <div class="tool-tabs" v-if="toolResponse">
        <div 
          class="tool-tab-item" 
          :class="{ active: activeTab === 'request' }"
          @click="activeTab = 'request'"
        >
          调用请求
        </div>
        <div 
          class="tool-tab-item" 
          :class="{ active: activeTab === 'response' }"
          @click="activeTab = 'response'"
        >
          执行结果
        </div>
      </div>

      <!-- 请求内容 -->
      <div v-show="activeTab === 'request'">
        <!-- 工具ID -->
        <div class="tool-info-row" v-if="showId">
          <span class="info-label">ID:</span>
          <span class="info-value">{{ toolCall.id }}</span>
        </div>

        <!-- 工具名称 -->
        <div class="tool-info-row">
          <span class="info-label">工具名称:</span>
          <span class="info-value tool-name">{{ toolCall.name }}</span>
        </div>

        <!-- 参数 -->
        <div class="tool-arguments">
          <div class="arguments-label">
            <span>参数:</span>
            <el-button 
              size="small" 
              text 
              @click="copyContent(formattedArguments)"
              style="margin-left: auto;"
            >
              <el-icon><DocumentCopy /></el-icon>
              复制
            </el-button>
          </div>
          <pre class="arguments-content">{{ formattedArguments }}</pre>
        </div>
      </div>

      <!-- 响应内容 -->
      <div v-show="activeTab === 'response'" class="tool-response">
         <!-- 可视化媒体展示 -->
         <div v-if="mediaContent" class="media-content">
           <!-- 图片 -->
           <div v-if="mediaContent.type === 'image'" class="media-item">
             <el-image 
               :src="mediaContent.url" 
               :preview-src-list="[mediaContent.url]"
               fit="contain"
               class="tool-image"
             >
               <template #error>
                 <div class="media-error">
                   <el-icon><Picture /></el-icon>
                   <span>图片加载失败</span>
                 </div>
               </template>
             </el-image>
           </div>
           <!-- 音频 -->
           <div v-else-if="mediaContent.type === 'audio'" class="media-item">
             <audio :src="mediaContent.url" controls class="tool-audio"></audio>
           </div>
         </div>

         <div class="tool-arguments">
          <div class="arguments-label">
            <span>结果数据:</span>
            <el-button 
              size="small" 
              text 
              @click="copyContent(formattedResponse)"
              style="margin-left: auto;"
            >
              <el-icon><DocumentCopy /></el-icon>
              复制
            </el-button>
          </div>
          <pre class="arguments-content response-content">{{ formattedResponse }}</pre>
        </div>
      </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Tools, DocumentCopy, Picture, ArrowRight, Select, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface ToolCall {
  id: string
  name: string
  arguments: string
}

interface ToolResponse {
  id: string
  name: string
  responseData: string
}

interface Props {
  toolCall: ToolCall
  toolResponse?: ToolResponse
  showId?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showId: false
})

const isCollapsed = ref(false)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 监听响应，如果有响应则自动折叠
watch(() => props.toolResponse, (newVal) => {
  if (newVal) {
    isCollapsed.value = true
  }
}, { immediate: true })

const activeTab = ref('request')

// 计算属性 - 媒体内容
const mediaContent = computed(() => {
  if (!props.toolResponse?.responseData) return null
  try {
    const data = JSON.parse(props.toolResponse.responseData)
    if (data.imageUrl) {
      return { type: 'image', url: data.imageUrl }
    }
    if (data.musicUrl) {
      return { type: 'audio', url: data.musicUrl }
    }
    return null
  } catch {
    return null
  }
})

// 计算属性 - 格式化参数
const formattedArguments = computed(() => {
  try {
    const parsed = JSON.parse(props.toolCall.arguments)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return props.toolCall.arguments
  }
})

// 计算属性 - 格式化响应
const formattedResponse = computed(() => {
  if (!props.toolResponse) return ''
  try {
    const parsed = JSON.parse(props.toolResponse.responseData)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return props.toolResponse?.responseData || ''
  }
})

// 方法 - 复制内容
const copyContent = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('内容已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.tool-call-card {
  background: #f7f8fa;
  border: 1px solid #e4e7ed;
  border-left: 4px solid #409eff;
  border-radius: 8px;
  margin: 12px 0;
  overflow: hidden;
  transition: all 0.3s;
}

.tool-call-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-left-color: #66b1ff;
}

.tool-call-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #ecf5ff;
  border-bottom: 1px solid #d9ecff;
  cursor: pointer;
  user-select: none;
}

.collapse-icon {
  margin-left: auto;
  color: #909399;
  transition: transform 0.3s;
  transform: rotate(90deg);
}

.collapse-icon.is-collapsed {
  transform: rotate(0deg);
}

/* Tabs 样式 */
.tool-tabs {
  display: flex;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.tool-tab-item {
  padding: 8px 16px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tool-tab-item:hover {
  color: #409eff;
}

.tool-tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 500;
}

.tool-icon {
  width: 24px;
  height: 24px;
  background: #409eff;
  color: white;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}


.tool-label {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}

.tool-status {
  font-size: 13px;
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.tool-status-success {
  color: #67c23a;
}

.tool-status-loading {
  color: #409eff;
}

.is-loading {
  animation: rotating 1.5s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}


.tool-call-body {
  padding: 16px;
}

.tool-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
}

.info-label {
  color: #909399;
  font-weight: 500;
  min-width: 80px;
}

.info-value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.tool-name {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  background: #f0f2f5;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 600;
  color: #e6a23c;
}

.tool-arguments {
  margin-top: 12px;
}

.arguments-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.arguments-content {
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
  max-height: 300px;
  overflow-y: auto;
}

/* 滚动条样式 */
.arguments-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.arguments-content::-webkit-scrollbar-thumb {
  background: #4b5263;
  border-radius: 4px;
}

.arguments-content::-webkit-scrollbar-thumb:hover {
  background: #5c6370;
}

.arguments-content::-webkit-scrollbar-track {
  background: #21252b;
}

/* 代码高亮样式 */
.arguments-content :deep(string) {
  color: #98c379;
}

.arguments-content :deep(number) {
  color: #d19a66;
}

.arguments-content :deep(boolean) {
  color: #56b6c2;
}

.arguments-content :deep(null) {
  color: #c678dd;
}

/* 媒体展示样式 */
.media-content {
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
}

.media-item {
  width: 100%;
  display: flex;
  justify-content: center;
}

.tool-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
}

.tool-audio {
  width: 100%;
  max-width: 400px;
}

.media-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 8px;
}
</style>

