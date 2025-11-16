<template>
  <div class="code-editor-wrapper">
    <div ref="editorRef" class="code-editor"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { oneDark } from '@codemirror/theme-one-dark'

interface Props {
  modelValue: string
  language?: 'javascript' | 'python'
  placeholder?: string
  readonly?: boolean
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  language: 'javascript',
  placeholder: '请输入代码...',
  readonly: false,
  height: '300px'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

const editorRef = ref<HTMLElement | null>(null)
let editorView: EditorView | null = null

// 获取语言扩展
const getLanguageExtension = () => {
  switch (props.language) {
    case 'python':
      return python()
    case 'javascript':
    default:
      return javascript()
  }
}

// 初始化编辑器
const initEditor = () => {
  if (!editorRef.value) return

  const startState = EditorState.create({
    doc: props.modelValue,
    extensions: [
      basicSetup,
      getLanguageExtension(),
      oneDark,
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          const newValue = update.state.doc.toString()
          emit('update:modelValue', newValue)
          emit('change', newValue)
        }
      }),
      EditorView.editable.of(!props.readonly),
      EditorView.lineWrapping, // 启用自动换行
      EditorView.theme({
        '&': {
          height: props.height,
          fontSize: '14px',
          border: 'none'
        },
        '.cm-scroller': {
          overflow: 'auto', // 同时支持水平和垂直滚动
          fontFamily: 'Consolas, Monaco, "Courier New", monospace',
          height: '100%'
        },
        '.cm-content': {
          padding: '8px 0',
          minHeight: '100%'
        },
        '.cm-line': {
          padding: '0 8px'
        }
      })
    ]
  })

  editorView = new EditorView({
    state: startState,
    parent: editorRef.value
  })
}

// 监听语言变化，重新创建编辑器
watch(() => props.language, () => {
  if (editorView) {
    editorView.destroy()
    initEditor()
  }
})

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  if (editorView && newValue !== editorView.state.doc.toString()) {
    editorView.dispatch({
      changes: {
        from: 0,
        to: editorView.state.doc.length,
        insert: newValue
      }
    })
  }
})

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  if (editorView) {
    editorView.destroy()
  }
})
</script>

<style scoped>
.code-editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  width: 100%;
}

.code-editor {
  width: 100%;
  height: 100%;
}

/* 深色主题样式调整 */
:deep(.cm-editor) {
  outline: none;
  height: 100%;
}

:deep(.cm-focused) {
  outline: none;
}

/* 确保滚动条可见 */
:deep(.cm-scroller) {
  overflow-x: auto !important;
  overflow-y: auto !important;
}

/* 优化滚动条样式 */
:deep(.cm-scroller::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

:deep(.cm-scroller::-webkit-scrollbar-track) {
  background: rgba(255, 255, 255, 0.05);
}

:deep(.cm-scroller::-webkit-scrollbar-thumb) {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb:hover) {
  background: rgba(255, 255, 255, 0.3);
}
</style>
