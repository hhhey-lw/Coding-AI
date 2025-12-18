<template>
  <el-drawer
    v-model="visible"
    title="End Configuration"
    direction="rtl"
    size="500px"
    :before-close="handleClose"
  >
    <div class="drawer-content">
      <el-form :model="nodeData" label-position="top">
        
        <div class="section-container">
            <div class="section-header">
                <span class="required-label">Message</span>
                <el-tooltip content="支持引用变量" placement="top">
                  <span class="variable-hint" v-pre>{{x}}</span>
                </el-tooltip>
                <div class="spacer"></div>
            </div>
            
            <div class="editor-container">
                <VariableInput
                    v-model="nodeData.message"
                    :variables="availableVariables"
                />
            </div>
        </div>

      </el-form>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { FullScreen } from '@element-plus/icons-vue'
import VariableInput from '@/components/common/VariableInput.vue'
import { useGraphVariables } from '@/composables/useGraphVariables'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'update:node'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const nodeId = computed(() => props.node?.id || '')
const { availableVariables } = useGraphVariables(nodeId)

const nodeData = ref({
    message: ''
})

const isInitializing = ref(false)

watch(() => props.node?.id, (newId) => {
  if (props.node && props.node.data) {
    isInitializing.value = true
    const data = props.node.data
    nodeData.value = {
        message: data.message || ''
    }
    nextTick(() => {
        isInitializing.value = false
    })
  }
}, { immediate: true })

watch(nodeData, (newVal) => {
    if (isInitializing.value) return
    if (props.node) {
        props.node.data = {
            ...props.node.data,
            message: newVal.message
        }
    }
}, { deep: true })

const handleClose = (done: () => void) => {
  done()
}

</script>

<style scoped>
.drawer-content {
  padding: 0 10px;
}

.section-container {
    margin-bottom: 24px;
}

.section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 14px;
    color: #303133;
}

.required-label::after {
    content: " *";
    color: red;
}

.spacer {
    flex-grow: 1;
}

.variable-hint {
    color: #409eff;
    font-size: 12px;
    margin-left: 8px;
    cursor: help;
}

.editor-container {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    overflow: hidden;
}

.editor-container:hover {
    border-color: #c0c4cc;
}

.editor-container:focus-within {
    border-color: #409eff;
}

:deep(.el-textarea__inner) {
    box-shadow: none;
    padding: 12px;
}

</style>
