<template>
  <div class="tool-call-card">
    <div class="tool-call-header">
      <div class="tool-icon">
        <el-icon :size="16"><Tools /></el-icon>
      </div>
      <span class="tool-label">工具调用</span>
      <el-tag size="small" type="info" effect="plain">{{ toolCall.name }}</el-tag>
    </div>

    <div class="tool-call-body">
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
            @click="copyArguments"
            style="margin-left: auto;"
          >
            <el-icon><DocumentCopy /></el-icon>
            复制
          </el-button>
        </div>
        <pre class="arguments-content">{{ formattedArguments }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Tools, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface ToolCall {
  id: string
  name: string
  arguments: string
}

interface Props {
  toolCall: ToolCall
  showId?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showId: false
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

// 方法 - 复制参数
const copyArguments = async () => {
  try {
    await navigator.clipboard.writeText(formattedArguments.value)
    ElMessage.success('参数已复制到剪贴板')
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
</style>

