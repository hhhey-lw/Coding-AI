<template>
  <div 
    class="custom-node"
    :class="{
      'node-selected': selected,
      [`node-${nodeType.toLowerCase()}`]: true
    }"
    @click="handleNodeClick"
  >
    <!-- 节点头部 -->
    <div class="node-header">
      <div class="node-icon">
        <el-icon :size="16">
          <component :is="getNodeIcon(nodeType)" />
        </el-icon>
      </div>
      <div class="node-title">{{ nodeData.name || getDefaultNodeName(nodeType) }}</div>
      <div class="node-actions">
        <el-button
          type="text"
          size="small"
          @click.stop="handleDelete"
          class="delete-btn"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 输入参数显示 - Start节点不显示输入，只显示输出 -->
    <div v-if="inputParams.length > 0 && nodeType !== 'Start'" class="node-section">
      <div class="section-title">输入</div>
      <div class="param-list">
        <div 
          v-for="param in inputParams" 
          :key="param.key"
          class="param-item input-param"
        >
          <span class="param-key">{{ param.key }}</span>
          <span class="param-type">{{ param.type || 'string' }}</span>
        </div>
      </div>
    </div>

    <!-- Judge节点特殊显示 - 分支列表 -->
    <div v-if="nodeType === 'Judge' && judgeBranches.length > 0" class="node-section judge-branches-section">
      <div class="section-title">配置</div>
      <div class="branches-display-list">
        <div 
          v-for="(branch, index) in judgeBranches" 
          :key="index"
          class="branch-display-item"
        >
          <div class="branch-content">
            <span class="branch-display-label">{{ getBranchLabel(index, judgeBranches.length) }}</span>
            <span class="branch-display-condition">{{ getBranchConditionText(branch) }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 节点配置显示 -->
    <div v-else-if="nodeConfigKeys.length > 0" class="node-section">
      <div class="section-title">配置</div>
      <div class="config-list">
        <div 
          v-for="key in nodeConfigKeys.slice(0, 2)" 
          :key="key"
          class="config-item"
        >
          <span class="config-key">{{ key }}</span>
          <span class="config-type">{{ getConfigValueType(key) }}</span>
        </div>
        <div v-if="nodeConfigKeys.length > 2" class="config-more">
          +{{ nodeConfigKeys.length - 2 }} more
        </div>
      </div>
    </div>

    <!-- 输出参数显示 -->
    <!-- Start节点特殊处理：显示为"输入"而不是"输出" -->
    <div v-if="outputParams.length > 0" class="node-section">
      <div class="section-title">{{ nodeType === 'Start' ? '输入' : '输出' }}</div>
      <div class="param-list">
        <div 
          v-for="param in outputParams" 
          :key="param.key"
          class="param-item output-param"
        >
          <span class="param-key">{{ param.key }}</span>
          <span class="param-type">{{ param.type || 'string' }}</span>
        </div>
      </div>
    </div>

    <!-- 左侧输入连接点 - 开始节点不显示 -->
    <Handle
      v-if="nodeType !== 'Start'"
      :id="`${nodeId}-input`"
      type="target"
      :position="Position.Left"
    />

    <!-- 右侧输出连接点 - 结束节点不显示 -->
    <!-- Judge节点有多个输出点，其他节点只有一个 -->
    <template v-if="nodeType === 'Judge'">
      <Handle
        v-for="(branch, index) in judgeBranches"
        :key="`branch-${index}`"
        :id="`${nodeId}-output-${index}`"
        type="source"
        :position="Position.Right"
        :style="{ top: `${getBranchHandlePositionAligned(index)}px` }"
        class="branch-handle"
      />
    </template>
    <Handle
      v-else-if="nodeType !== 'End'"
      :id="`${nodeId}-output`"
      type="source"
      :position="Position.Right"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import type { Node, NodeType } from '@/types/workflow'

// Props
interface Props {
  nodeId: string
  nodeType: string
  nodeData: any
  selected?: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'node-click': [nodeId: string]
  'node-delete': [nodeId: string]
}>()

// 计算属性：输入参数
const inputParams = computed(() => {
  return props.nodeData.config?.input_params || []
})

// 计算属性：输出参数
const outputParams = computed(() => {
  return props.nodeData.config?.output_params || []
})

// 计算属性：节点配置键
const nodeConfigKeys = computed(() => {
  const nodeParam = props.nodeData.config?.node_param || {}
  return Object.keys(nodeParam).filter(key => {
    const value = nodeParam[key]
    // 过滤掉 id 和空串
    return key !== 'id' && value !== '' && value !== null && value !== undefined
  })
})

// 计算属性：Judge节点的分支列表
const judgeBranches = computed(() => {
  if (props.nodeType !== 'Judge') return []
  const branches = props.nodeData.config?.node_param?.branches || []
  // 至少显示一个分支
  return branches.length > 0 ? branches : [{ id: '', conditions: [] }]
})

// 计算Judge节点分支连接点的对齐位置（与分支行对齐）
const getBranchHandlePositionAligned = (index: number): number => {
  // 节点头部高度 padding: 8px 12px 约 36px
  // section-title 高度约 28px
  // 每个 branch-display-item: min-height: 24px + padding: 4px 0 + gap: 4px = 32px
  const headerHeight = 36
  const sectionTitleHeight = 28
  const branchItemHeight = 28  // 24 + 4
  const firstItemOffset = 12 // 第一行的中心偏移
  
  return headerHeight + sectionTitleHeight + firstItemOffset + (index * branchItemHeight)
}

// 获取分支标签
const getBranchLabel = (index: number, total: number): string => {
  if (total === 1) return 'IF'
  if (index === 0) return 'IF'
  if (index === total - 1) {
    // 检查是否为ELSE分支（没有条件）
    const branch = judgeBranches.value[index]
    if (!branch.conditions || branch.conditions.length === 0) {
      return 'ELSE'
    }
    return 'IF ELSE'
  }
  return 'IF ELSE'
}

// 获取分支条件的文本描述
const getBranchConditionText = (branch: any): string => {
  if (!branch.conditions || branch.conditions.length === 0) {
    return '默认'
  }
  
  const condition = branch.conditions[0]
  // 提取变量名，去掉${...}包装
  let leftValue = condition.left_key?.value || '?'
  if (leftValue.startsWith('${') && leftValue.endsWith('}')) {
    leftValue = leftValue.slice(2, -1)
  }
  
  // 简化变量名显示，只显示最后一部分
  const simplifiedVar = leftValue.includes('.') 
    ? leftValue.split('.').pop() 
    : leftValue
  
  const operator = condition.operator || 'equals'
  const rightValue = condition.right_value || ''
  
  const operatorText: Record<string, string> = {
    'isNull': '为空',
    'isNotNull': '不为空',
    'equals': '=',
    'notEquals': '≠'
  }
  
  if (operator === 'isNull' || operator === 'isNotNull') {
    return `${simplifiedVar} ${operatorText[operator]}`
  }
  
  return `${simplifiedVar} ${operatorText[operator]} ${rightValue}`
}

// 获取配置值的类型
const getConfigValueType = (key: string) => {
  const nodeParam = props.nodeData.config?.node_param || {}
  const value = nodeParam[key]
  
  if (value === null || value === undefined || value === '') {
    return 'string'
  }
  
  if (Array.isArray(value)) {
    return 'array'
  }
  
  const type = typeof value
  if (type === 'object') {
    return 'object'
  }
  
  return type // 'string', 'number', 'boolean'
}

// 获取节点图标
const getNodeIcon = (nodeType: string) => {
  const iconMap: Record<string, string> = {
    'Start': 'VideoPlay',
    'End': 'VideoPause',
    'TextGen': 'ChatDotRound',
    'MCP': 'Connection',
    'ImgGen': 'Picture',
    'VideoGen': 'VideoCamera',
    'MusicGen': 'Headphone',
    'Script': 'DocumentCopy',
    'Email': 'Message',
    'Judge': 'Switch',
    'Output': 'Upload'
  }
  return iconMap[nodeType] || 'Box'
}

// 获取默认节点名称
const getDefaultNodeName = (nodeType: string) => {
  const nameMap: Record<string, string> = {
    'Start': '开始',
    'End': '结束',
    'TextGen': 'LLM处理',
    'MCP': 'MCP调用',
    'ImgGen': '图像生成',
    'VideoGen': '视频生成',
    'MusicGen': '音乐生成',
    'Script': '脚本执行',
    'Email': '发送邮件',
    'Judge': '条件判断',
    'Output': '数据输出'
  }
  return nameMap[nodeType] || nodeType
}

// 事件处理
const handleNodeClick = () => {
  emit('node-click', props.nodeId)
}

const handleDelete = () => {
  emit('node-delete', props.nodeId)
}
</script>

<style scoped>
.custom-node {
  min-width: 200px;
  max-width: 280px;
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 9px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.custom-node:hover {
  border: 4px solid rgb(175, 149, 235);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.2);
}

.node-selected {
  border: 4px solid rgb(175, 149, 235) !important;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) !important;
}

/* 节点类型样式 */
.node-start .node-header {
  background: #FFC6D8;
}
.node-end .node-header {
  background: #FFC6D8;
}
.node-start, .node-end {
  border: 3px solid rgb(233, 165, 187);
}

.node-musicgen .node-header {
  background:rgb(227, 218, 242);
}
.node-musicgen {
  border: 3px solid rgb(184, 164, 215);
}

.node-videogen .node-header {
  background: #f78989;
}
.node-videogen {
  border: 3px solid rgb(220, 105, 105);
}

.node-textgen .node-header {
  background: #B5E4CA;
}
.node-textgen {
  border: 3px solid rgb(128, 210, 165);
}

.node-imggen .node-header {
  background: #FFD88D;
}
.node-imggen {
  border: 3px solid rgb(242, 188, 87);
}

.node-judge .node-header {
  background: #B9E5E8;
}
.node-judge {
  border: 3px solid rgb(117, 225, 233);
}

.node-mcp .node-header {
  background: #FFCDB2;
}
.node-mcp {
  border: 3px solid rgb(242, 167, 127);
}

.node-script .node-header {
  background: #D0E8C5;
}
.node-script {
  border: 3px solid rgb(173, 227, 148);
}

.node-email .node-header {
  background: #F5CBCB;
}
.node-email {
  border: 3px solid rgb(236, 165, 165);
}

.node-header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: linear-gradient(135deg, #909399, #b1b3b8);
  color: #1A1D1F;
  border-radius: 6px 6px 0 0;
  font-weight: 500;
  font-size: 14px;
}

.node-icon {
  margin-right: 8px;
  display: flex;
  align-items: center;
}

.node-title {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.custom-node:hover .node-actions {
  opacity: 1;
}

.delete-btn {
  padding: 2px;
  font-size: 16px;
  font-weight: 700;
  color: rgb(217, 104, 104);
}

.delete-btn:hover {
  color: rgb(207, 57, 57);
}

.node-section {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.node-section:last-child {
  border-bottom: none;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 6px 0;
  position: relative;
  font-size: 12px;
}

.input-param {
  justify-content: flex-start;
}

.output-param {
  justify-content: flex-start;
}

.param-key {
  font-weight: 500;
  color: #303133;
}

.param-type {
  margin-left: 8px;
  font-size: 11px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 6px;
}

.output-param .param-type {
  margin-left: 8px;
  margin-right: 0;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-item {
  font-size: 12px;
  color: #606266;
  padding: 6px 0;
  display: flex;
  align-items: center;
}

.config-key {
  font-weight: 500;
  color: #303133;
}

.config-type {
  margin-left: 8px;
  font-size: 11px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 6px;
}

.config-more {
  font-size: 11px;
  color: #909399;
  font-style: italic;
  padding: 6px 0;
}

/* Judge节点分支显示样式 */
.judge-branches-section {
  padding-right: 8px; /* 为右侧连接点留出空间 */
}

.branches-display-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.branch-display-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
  font-size: 12px;
  position: relative;
  min-height: 24px;
}

.branch-content {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  padding-right: 16px; /* 为右侧连接点留空间 */
}

.branch-display-label {
  font-weight: 600;
  color: #409eff;
  font-size: 11px;
  min-width: 30px;
  flex-shrink: 0;
}

.branch-display-condition {
  color: #606266;
  font-size: 11px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Handle连接点样式 - 使用Vue Flow默认样式 */
:deep(.vue-flow__handle) {
  width: 10px;
  height: 10px;
  border: 2px solid white;
  background: #555;
}

:deep(.vue-flow__handle-left) {
  left: -5px;
  top: 50%;
  transform: translateY(-50%);
}

:deep(.vue-flow__handle-right) {
  right: -5px;
  top: 50%;
  transform: translateY(-50%);
}

:deep(.vue-flow__handle-top) {
  left: 50%;
  top: -5px;
  transform: translateX(-50%);
}

:deep(.vue-flow__handle-bottom) {
  left: 50%;
  bottom: -5px;
  transform: translateX(-50%);
}

:deep(.vue-flow__handle:hover) {
  background: #409eff;
}

:deep(.vue-flow__handle.connecting),
:deep(.vue-flow__handle.valid) {
  background: #67c23a;
}

/* Judge节点分支连接点样式 */
:deep(.branch-handle) {
  width: 10px;
  height: 10px;
  border: 2px solid white;
  background: #409eff;
  right: -5px;
  transform: translateY(-50%);
}

:deep(.branch-handle:hover) {
  width: 12px;
  height: 12px;
  right: -6px;
  background: #67c23a;
  transform: translateY(-50%);
}
</style>
