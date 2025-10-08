<template>
  <div 
    class="variable-input-wrapper" 
    ref="wrapperRef"
    :class="$attrs.size ? `variable-input--${$attrs.size}` : ''"
  >
    <!-- wangEditor 编辑器（所有输入都用它） -->
    <div 
      ref="editorContainer"
      class="wang-editor-container"
      :class="{ 
        'is-small': $attrs.size === 'small',
        'is-single-line': !isTextarea
      }"
      :style="editorStyle"
    ></div>
    
    <!-- 变量列表悬浮框 -->
    <teleport to="body">
      <div
        v-if="showVariableList"
        class="variable-list-popover"
        :style="popoverStyle"
        @mousedown.prevent
      >
        <div class="variable-list-header">
          <el-icon><Link /></el-icon>
          <span>可用变量</span>
        </div>
        
        <div v-if="filteredVariables.length === 0" class="variable-list-empty">
          <el-icon><Warning /></el-icon>
          <span>暂无可用变量</span>
        </div>
        
        <div v-else class="variable-list-content">
          <div
            v-for="(variable, index) in filteredVariables"
            :key="variable.value"
            class="variable-item"
            :class="{ 'is-active': index === selectedIndex }"
            @click="selectVariable(variable)"
            @mouseenter="selectedIndex = index"
          >
            <div class="variable-label">
              <el-icon class="variable-icon"><Connection /></el-icon>
              <span class="variable-name">{{ variable.label }}</span>
            </div>
            <div class="variable-meta">
              <span class="variable-type">{{ variable.type || 'string' }}</span>
              <span class="variable-node">{{ variable.nodeName }}</span>
            </div>
          </div>
        </div>
        
        <div class="variable-list-footer">
          <span class="hint-text">↑↓ 选择 • Enter 确认 • Esc 取消</span>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted, onBeforeUnmount } from 'vue'
import { createEditor, IEditorConfig } from '@wangeditor/editor'
import type { IDomEditor } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'

interface VariableOption {
  label: string
  value: string
  nodeId: string
  nodeName: string
  type?: string
}

interface Props {
  modelValue: string
  availableVariables?: VariableOption[]
}

const props = withDefaults(defineProps<Props>(), {
  availableVariables: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'variable-select': [variable: VariableOption]
}>()

// 响应式数据
const localValue = ref('')
const showVariableList = ref(false)
const selectedIndex = ref(0)
const triggerPosition = ref({ x: 0, y: 0 })
const lastDollarIndex = ref(-1)
const wrapperRef = ref<HTMLElement | null>(null)
const editorContainer = ref<HTMLElement | null>(null)
let editorInstance: IDomEditor | null = null
let isUpdating = ref(false) // 防止循环更新

// 计算属性
const filteredVariables = computed(() => {
  if (!showVariableList.value) return []
  
  // 获取 $ 后面的文本作为过滤条件
  const input = localValue.value || ''
  const dollarIndex = lastDollarIndex.value
  
  if (dollarIndex === -1) return props.availableVariables
  
  const searchText = input.substring(dollarIndex + 1).toLowerCase()
  
  if (!searchText) return props.availableVariables
  
  return props.availableVariables.filter(v => 
    v.label.toLowerCase().includes(searchText) ||
    v.nodeName.toLowerCase().includes(searchText)
  )
})

const popoverStyle = computed(() => {
  return {
    left: `${triggerPosition.value.x}px`,
    top: `${triggerPosition.value.y}px`
  }
})

// 检测是否为 textarea（多行）- 默认多行
const isTextarea = computed(() => {
  return true
})

// 编辑器样式（根据 rows 动态设置高度）
const editorStyle = computed(() => {
  const attrs = props as any
  const rows = (attrs.rows ?? 2)
  
  if (!isTextarea.value) {
    // 单行模式
    return {}
  }
  
  // 多行模式，根据 rows 计算高度
  if (rows > 0) {
    const lineHeight = attrs.size === 'small' ? 20 : 22
    const minHeight = rows * lineHeight + 10 // 加上padding
    return {
      '--editor-min-height': `${minHeight}px`
    }
  }
  
  return {}
})

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  localValue.value = newValue || ''
  
  // 同步到编辑器（避免循环更新）
  if (editorInstance && !isUpdating.value) {
    const currentText = editorInstance.getText().trim()
    if (currentText !== newValue) {
      isUpdating.value = true
      editorInstance.setHtml(convertTextToHtml(newValue || ''))
      nextTick(() => {
        isUpdating.value = false
      })
    }
  }
}, { immediate: true })


// 键盘事件处理
const handleKeydown = (event: KeyboardEvent) => {
  if (!showVariableList.value) return
  
  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, filteredVariables.value.length - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      break
    case 'Enter':
      event.preventDefault()
      if (filteredVariables.value[selectedIndex.value]) {
        selectVariable(filteredVariables.value[selectedIndex.value])
      }
      break
    case 'Escape':
      event.preventDefault()
      showVariableList.value = false
      break
  }
}

// 选择变量
const selectVariable = (variable: VariableOption) => {
  if (!editorInstance) return
  
  const dollarIndex = lastDollarIndex.value
  if (dollarIndex === -1) return
  
  showVariableList.value = false
  isUpdating.value = true
  
  // 获取纯文本
  const text = editorInstance.getText()
  
  // 找到 $ 后面已输入的内容
  const before = text.substring(0, dollarIndex)
  const afterDollar = text.substring(dollarIndex + 1)
  
  // 找到 $ 后面已输入的部分（可能是 ${xx 或 {xx 这种不完整的）
  const searchMatch = afterDollar.match(/^(\{[^}\s]*)?/)
  const typedPart = searchMatch ? searchMatch[0] : ''
  const after = afterDollar.substring(typedPart.length)
  
  // 构建新文本 - 在变量后面添加一个空格
  const newText = before + variable.value + ' ' + after
  
  // 重新转换整个内容为HTML（会给所有变量添加高亮）
  const newHtml = convertTextToHtml(newText)
  
  // 更新编辑器
  editorInstance.setHtml(newHtml)
  
  localValue.value = newText
  emit('update:modelValue', newText)
  emit('variable-select', variable)
  
  // 聚焦并将光标移到变量后面
  nextTick(() => {
    if (!editorInstance) return
    
    editorInstance.focus()
    
    // 计算光标应该在的位置（变量+空格后面）
    const cursorPosition = before.length + variable.value.length + 1
    
    // 使用 wangEditor 的 move 方法移动光标
    // move 方法需要传入相对于当前位置的偏移量
    // 先移到开头，再移到目标位置
    editorInstance.select([])
    
    // 直接设置 selection
    setTimeout(() => {
      if (!editorInstance) return
      try {
        // 获取编辑器的文本节点
        const editorElem = editorContainer.value?.querySelector('.w-e-text-container [contenteditable]')
        if (editorElem) {
          const range = document.createRange()
          const sel = window.getSelection()
          
          // 遍历查找文本节点并设置光标
          let currentPos = 0
          const walk = (node: Node): boolean => {
            if (node.nodeType === Node.TEXT_NODE) {
              const textLength = node.textContent?.length || 0
              if (currentPos + textLength >= cursorPosition) {
                range.setStart(node, cursorPosition - currentPos)
                range.collapse(true)
                sel?.removeAllRanges()
                sel?.addRange(range)
                return true
              }
              currentPos += textLength
            } else if (node.nodeType === Node.ELEMENT_NODE) {
              // 跳过变量高亮的 span，但计入其文本长度
              if ((node as Element).classList.contains('var-highlight')) {
                const textLength = node.textContent?.length || 0
                if (currentPos + textLength >= cursorPosition) {
                  // 光标在变量内部或后面，移到变量后
                  const nextNode = node.nextSibling
                  if (nextNode) {
                    range.setStart(nextNode, 0)
                  } else {
                    range.setStartAfter(node)
                  }
                  range.collapse(true)
                  sel?.removeAllRanges()
                  sel?.addRange(range)
                  return true
                }
                currentPos += textLength
              } else {
                for (const child of Array.from(node.childNodes)) {
                  if (walk(child)) return true
                }
              }
            }
            return false
          }
          
          walk(editorElem)
        }
      } catch (e) {
        console.error('设置光标位置失败:', e)
      }
      
      isUpdating.value = false
    }, 50)
  })
}


// 点击外部关闭
const handleClickOutside = (event: MouseEvent) => {
  if (!showVariableList.value) return
  
  const target = event.target as HTMLElement
  const popover = document.querySelector('.variable-list-popover')
  const wrapper = wrapperRef.value
  
  if (popover && !popover.contains(target) && wrapper && !wrapper.contains(target)) {
    showVariableList.value = false
  }
}

// 初始化编辑器
const initEditor = () => {
  if (!editorContainer.value) return
  
  const editorConfig: Partial<IEditorConfig> = {
    placeholder: (props as any).placeholder || '请输入内容，支持 $ 引用变量',
    readOnly: !!(props as any).disabled,
    onChange: (editor: IDomEditor) => {
      if (isUpdating.value) return
      
      const text = editor.getText()
      localValue.value = text
      emit('update:modelValue', text)
      
      // 检测是否输入了 $
      if (text.endsWith('$')) {
        lastDollarIndex.value = text.length - 1
        showVariableList.value = true
        selectedIndex.value = 0
        
        nextTick(() => {
          updateEditorPopoverPosition()
        })
      } else if (showVariableList.value) {
        const currentDollarIndex = text.lastIndexOf('$')
        if (currentDollarIndex === -1) {
          showVariableList.value = false
        } else {
          lastDollarIndex.value = currentDollarIndex
        }
      }
    }
  }
  
  editorInstance = createEditor({
    selector: editorContainer.value,
    config: editorConfig,
    mode: 'simple'
  })
  
  // 设置初始内容
  if (localValue.value) {
    editorInstance.setHtml(convertTextToHtml(localValue.value))
  }
  
  // 等待编辑器渲染完成后监听键盘事件
  nextTick(() => {
    const editorTextArea = editorContainer.value?.querySelector('.w-e-text-container [contenteditable]')
    if (editorTextArea) {
      editorTextArea.addEventListener('keydown', (event) => {
        if (showVariableList.value) {
          const e = event as KeyboardEvent
          // 阻止编辑器默认行为
          if (['ArrowDown', 'ArrowUp', 'Enter', 'Escape'].includes(e.key)) {
            e.stopPropagation()
            e.preventDefault()
            handleKeydown(e)
          }
        }
      }, true) // 使用捕获阶段
    }
  })
}


// 转换文本为HTML（仅用于初始化，保留已有的变量高亮）
const convertTextToHtml = (text: string): string => {
  if (!text) return '<p><br/></p>'
  
  // 转义 HTML
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 只在初始化时给变量添加高亮（用户通过选择插入的变量已经带样式了）
  html = html.replace(/\$\{([^}]+)\}/g, '<span class="var-highlight" style="background-color: #e1f0ff; color: #409eff; font-weight: 600; padding: 2px 6px; border-radius: 3px; font-family: Monaco, Menlo, Consolas, monospace; display: inline-block; margin: 0 2px;">${$1}</span>')
  
  // 处理换行
  const lines = html.split('\n')
  return lines.map(line => `<p>${line || '<br/>'}</p>`).join('')
}

// 更新编辑器弹出框位置
const updateEditorPopoverPosition = () => {
  if (!editorContainer.value) return
  
  const rect = editorContainer.value.getBoundingClientRect()
  
  triggerPosition.value = {
    x: rect.left,
    y: rect.bottom + 4
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  
  // 初始化编辑器（所有输入都使用编辑器）
  nextTick(() => {
    initEditor()
  })
})

onBeforeUnmount(() => {
  // 销毁编辑器
  if (editorInstance) {
    editorInstance.destroy()
    editorInstance = null
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.variable-input-wrapper {
  position: relative;
  width: 100%;
}

/* wangEditor 容器 */
.wang-editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
  background: #fff;
}

.wang-editor-container:hover {
  border-color: #c0c4cc;
}

.wang-editor-container:focus-within {
  border-color: #409eff;
}

/* 隐藏编辑器工具栏 */
.wang-editor-container :deep(.w-e-toolbar) {
  display: none !important;
}

/* 编辑器内容区样式 */
.wang-editor-container :deep(.w-e-text-container) {
  border: none !important;
}

.wang-editor-container :deep(.w-e-text-placeholder) {
  top: 5px;
  left: 8px;
  font-style: normal;
  color: #c0c4cc;
  padding-left: 4px;
  display: flex;
  align-items: center;
}

/* 多行模式（textarea）- 根据 rows 限制高度 */
.wang-editor-container :deep(.w-e-scroll) {
  min-height: var(--editor-min-height, 80px);
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0px;
  font-size: 14px;
  line-height: 1.5;
}

.wang-editor-container.is-small :deep(.w-e-scroll) {
  min-height: var(--editor-min-height, 60px);
  max-height: 350px;
  font-size: 13px;
}

/* 单行模式（input）- 限制高度，支持滚动 */
.wang-editor-container.is-single-line :deep(.w-e-scroll) {
  min-height: 32px;
  max-height: 80px;
  overflow-y: auto;
  padding: 4px 0px;
  line-height: 1.4;
}

.wang-editor-container.is-single-line.is-small :deep(.w-e-scroll) {
  min-height: 24px;
  max-height: 60px;
  padding: 3px 0px;
  font-size: 13px;
}

.wang-editor-container.is-single-line :deep(p) {
  margin: 0;
  padding: 0;
  line-height: 1.4;
}

/* wangEditor 中的变量高亮样式 */
.wang-editor-container :deep(p) {
  margin: 0;
  padding: 0;
  line-height: 1.5;
}

.wang-editor-container :deep(.var-highlight),
.wang-editor-container :deep(span[style*="background-color: #e1f0ff"]) {
  background-color: #e1f0ff !important;
  color: #409eff !important;
  font-weight: 600 !important;
  padding: 2px 6px !important;
  border-radius: 3px !important;
  font-family: Monaco, monospace !important;
  display: inline-block !important;
  margin: 0 1px !important;
}

/* 滚动条样式 */
.wang-editor-container :deep(.w-e-scroll)::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.wang-editor-container :deep(.w-e-scroll)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.wang-editor-container :deep(.w-e-scroll)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.wang-editor-container :deep(.w-e-scroll)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.variable-list-popover {
  position: fixed;
  z-index: 9999;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 300px;
  max-width: 400px;
  max-height: 400px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.variable-list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f6f8 100%);
  border-bottom: 1px solid #e4e7ed;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.variable-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.variable-list-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 16px;
  color: #909399;
  font-size: 13px;
}

.variable-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.variable-item:hover,
.variable-item.is-active {
  background: #ecf5ff;
}

.variable-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.variable-icon {
  color: #409eff;
  font-size: 16px;
  flex-shrink: 0;
}

.variable-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.variable-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 24px;
  font-size: 12px;
}

.variable-type {
  color: #86868b;
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.variable-node {
  color: #909399;
}

.variable-list-footer {
  padding: 8px 16px;
  background: #fafbfc;
  border-top: 1px solid #e4e7ed;
  font-size: 11px;
  color: #909399;
  text-align: center;
}

.hint-text {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

/* 滚动条样式 */
.variable-list-content::-webkit-scrollbar {
  width: 6px;
}

.variable-list-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.variable-list-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.variable-list-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

