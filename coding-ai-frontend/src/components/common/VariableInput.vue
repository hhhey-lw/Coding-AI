<template>
  <div class="variable-input-container">
    <div
      ref="editorRef"
      class="variable-editor"
      contenteditable="true"
      @input="handleInput"
      @keydown="handleKeydown"
      @blur="handleBlur"
      @focus="handleFocus"
      placeholder="Type {{ to reference variables"
    ></div>

    <div v-if="showPopover" class="variable-popover" :style="popoverStyle">
       <div class="popover-header">Variables</div>
       <div class="popover-content">
           <div 
             v-for="(item, index) in filteredVariables" 
             :key="index"
             class="variable-option"
             :class="{ active: activeIndex === index }"
             @mousedown.prevent="selectVariable(item)"
             @mouseenter="activeIndex = index"
           >
              <span class="var-label">{{ item.label }}</span>
              <span class="var-type">{{ item.type }}</span>
           </div>
           <div v-if="filteredVariables.length === 0" class="no-data">
               No variables found
           </div>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick, computed } from 'vue'
import type { VariableOption } from '@/composables/useGraphVariables'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  variables: {
    type: Array as () => VariableOption[],
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref<HTMLElement | null>(null)
const showPopover = ref(false)
const popoverStyle = ref({ top: '0px', left: '0px' })
const activeIndex = ref(0)
const query = ref('')
const cursorPosition = ref(0)

// Convert modelValue (plain text) to HTML with spans
const textToHtml = (text: string) => {
    if (!text) return ''
    // Replace {{...}} with span
    // Note: We assume variables match the exact value provided in options
    // But for general highlighting, we can just regex for {{...}}
    return text.replace(/\{\{([^}]+)\}\}/g, '<span class="variable" contenteditable="false">{{$1}}</span>')
}

// Convert HTML to plain text
const htmlToText = (html: string) => {
    const tmp = document.createElement('div')
    tmp.innerHTML = html
    return tmp.innerText
}

// Initial sync
onMounted(() => {
    if (editorRef.value) {
        editorRef.value.innerHTML = textToHtml(props.modelValue)
    }
})

watch(() => props.modelValue, (newVal) => {
    if (editorRef.value) {
        // If the element is focused, we assume the DOM is already consistent with the user's intent.
        // Forcing an update from the prop can cause cursor jumping, especially with contenteditable quirks.
        if (document.activeElement === editorRef.value) return

        if (htmlToText(editorRef.value.innerHTML) !== newVal) {
            editorRef.value.innerHTML = textToHtml(newVal)
        }
    }
})

const handleInput = (e: Event) => {
    const target = e.target as HTMLElement
    const text = target.innerText
    emit('update:modelValue', text)
    
    checkTrigger(target)
}

const checkTrigger = (target: HTMLElement) => {
    const selection = window.getSelection()
    if (!selection || !selection.rangeCount) return
    const range = selection.getRangeAt(0)
    
    // Check if we just typed '{' or '{{'
    // This logic is tricky. Simplification:
    // Find the text node where cursor is.
    const textNode = range.startContainer
    if (textNode.nodeType === Node.TEXT_NODE) {
        const textContent = textNode.textContent || ''
        const offset = range.startOffset
        
        // Check for {{ before cursor
        const lastTwo = textContent.slice(Math.max(0, offset - 2), offset)
        if (lastTwo === '{{') {
            // Trigger!
            showPopover.value = true
            updatePopoverPosition()
            query.value = ''
            cursorPosition.value = offset
            return
        }
        
        // If we are already showing, update query
        if (showPopover.value) {
            // Find where '{{' started
             // This is a bit complex to track context.
             // For now, let's just show popover on {{ and close on space or }
             const textBefore = textContent.slice(0, offset)
             const lastTrigger = textBefore.lastIndexOf('{{')
             if (lastTrigger !== -1) {
                 query.value = textBefore.slice(lastTrigger + 2)
                 // If query contains space or }, close
                 if (query.value.includes(' ') || query.value.includes('}')) {
                     showPopover.value = false
                 }
             } else {
                 showPopover.value = false
             }
        }
    } else {
        showPopover.value = false
    }
}

const updatePopoverPosition = () => {
    const selection = window.getSelection()
    if (!selection || !selection.rangeCount) return
    const range = selection.getRangeAt(0)
    const rect = range.getBoundingClientRect()
    
    // Relative to viewport, but we need relative to editor or body?
    // Using fixed position for popover usually works best
    // But scoped style might interfere.
    // Let's use fixed.
    popoverStyle.value = {
        top: `${rect.bottom + 5}px`,
        left: `${rect.left}px`
    }
}

const filteredVariables = computed(() => {
    if (!query.value) return props.variables
    const q = query.value.toLowerCase()
    return props.variables.filter(v => 
        v.label.toLowerCase().includes(q) || 
        v.value.toLowerCase().includes(q)
    )
})

const selectVariable = (item: VariableOption) => {
    const selection = window.getSelection()
    if (!selection || !selection.rangeCount) return
    const range = selection.getRangeAt(0)
    
    // We need to replace the '{{query' with the span
    // Find the text node
    const textNode = range.startContainer
    if (textNode.nodeType === Node.TEXT_NODE) {
        const textContent = textNode.textContent || ''
        const offset = range.startOffset
        const lastTrigger = textContent.slice(0, offset).lastIndexOf('{{')
        
        if (lastTrigger !== -1) {
             // Split node
             const beforeText = textContent.slice(0, lastTrigger)
             const afterText = textContent.slice(offset)
             
             const span = document.createElement('span')
             span.className = 'variable'
             span.contentEditable = 'false'
             span.innerText = item.value // e.g. {{start.foo}}
             
             // We need to replace textNode with before + span + after
             const parent = textNode.parentNode
             if (parent) {
                 if (beforeText) {
                     parent.insertBefore(document.createTextNode(beforeText), textNode)
                 }
                 parent.insertBefore(span, textNode)
                 // Add a space or zero-width space after to allow typing?
                 const space = document.createTextNode('\u00A0') // Nbsp
                 parent.insertBefore(space, textNode)
                 
                 if (afterText) {
                     parent.insertBefore(document.createTextNode(afterText), textNode)
                 }
                 
                 parent.removeChild(textNode)
                 
                 // Move cursor after space
                 const newRange = document.createRange()
                 newRange.setStart(space, 1)
                 newRange.collapse(true)
                 selection.removeAllRanges()
                 selection.addRange(newRange)
             }
        }
    }
    
    showPopover.value = false
    // Trigger update
    if (editorRef.value) {
        emit('update:modelValue', editorRef.value.innerText)
    }
}

const handleKeydown = (e: KeyboardEvent) => {
    if (showPopover.value) {
        if (e.key === 'ArrowDown') {
            e.preventDefault()
            activeIndex.value = (activeIndex.value + 1) % filteredVariables.value.length
        } else if (e.key === 'ArrowUp') {
            e.preventDefault()
            activeIndex.value = (activeIndex.value - 1 + filteredVariables.value.length) % filteredVariables.value.length
        } else if (e.key === 'Enter') {
            e.preventDefault()
            if (filteredVariables.value.length > 0) {
                selectVariable(filteredVariables.value[activeIndex.value])
            }
        } else if (e.key === 'Escape') {
            showPopover.value = false
        } else if (['Home', 'End', 'PageUp', 'PageDown'].includes(e.key)) {
            showPopover.value = false
        }
    }
}

const handleBlur = () => {
    // Delay hiding to allow click event on popover to fire
    setTimeout(() => {
        showPopover.value = false
    }, 200)
}

const handleFocus = () => {
    // 
}

</script>

<style>
.variable-input-container {
    position: relative;
    width: 100%;
}

.variable-editor {
    min-height: 100px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 8px 12px;
    outline: none;
    white-space: pre-wrap;
    line-height: 1.5;
    font-size: 14px;
    color: #606266;
}

.variable-editor:focus {
    border-color: #409eff;
}

/* Global style for variable span */
.variable {
    background-color: #ecf5ff;
    color: #409eff;
    border: 1px solid #d9ecff;
    border-radius: 4px;
    padding: 2px 4px;
    margin: 0 2px;
    font-size: 12px;
    user-select: none;
    cursor: default;
}

.variable-popover {
    position: fixed;
    background: white;
    border: 1px solid #e4e7ed;
    box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
    border-radius: 4px;
    z-index: 9999;
    min-width: 200px;
    max-height: 200px;
    overflow-y: auto;
}

.popover-header {
    padding: 8px 12px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;
    font-size: 12px;
    color: #909399;
}

.variable-option {
    padding: 8px 12px;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
}

.variable-option:hover, .variable-option.active {
    background-color: #f5f7fa;
}

.var-type {
    font-size: 12px;
    color: #909399;
    background: #f0f2f5;
    padding: 2px 6px;
    border-radius: 4px;
}

.no-data {
    padding: 12px;
    color: #909399;
    text-align: center;
    font-size: 13px;
}
</style>
