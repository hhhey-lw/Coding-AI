<template>
  <el-drawer
    v-model="visible"
    title="Direct Reply Configuration"
    direction="rtl"
    size="500px"
    :before-close="handleClose"
  >
    <div class="drawer-content">
      <el-form :model="nodeData" label-position="top">
        
        <div class="section-container">
            <div class="section-header">
                <span class="required-label">Message</span>
                <div class="spacer"></div>
                <div class="variable-icon">
                    <el-tooltip content="Type {{ to select variables" placement="top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="tabler-icon tabler-icon-variable" style="color: teal;"><path d="M5 4c-2.5 5 -2.5 10 0 16m14 -16c2.5 5 2.5 10 0 16m-10 -11h1c1 0 1 1 2.016 3.527c.984 2.473 .984 3.473 1.984 3.473h1"></path><path d="M8 16c1.5 0 3 -2 4 -3.5s2.5 -3.5 4 -3.5"></path></svg>
                    </el-tooltip>
                </div>
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

.variable-icon {
    display: flex;
    align-items: center;
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
