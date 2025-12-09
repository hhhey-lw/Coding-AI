<template>
  <el-drawer
    v-model="visible"
    :title="nodeData?.label || 'Human Input Configuration'"
    direction="rtl"
    size="450px"
    :before-close="handleClose"
    class="agent-drawer"
  >
    <template #header>
      <div class="drawer-header">
        <span class="drawer-title">{{ nodeData?.label }}</span>
        <el-button text circle>
          <el-icon><EditPen /></el-icon>
        </el-button>
      </div>
    </template>

    <div class="drawer-content" v-if="nodeData">
      <!-- Input Prompt -->
      <div class="form-section">
        <div class="section-label row-between">
          <span>Input Prompt <span class="required">*</span></span>
          <div class="content-tools">
             <el-icon><Key /></el-icon>
             <el-icon><FullScreen /></el-icon>
          </div>
        </div>
        <el-input 
          v-model="formData.inputPrompt" 
          type="textarea" 
          :rows="4" 
          resize="none"
          placeholder="Enter input prompt..."
        />
      </div>

      <!-- Enable Feedback -->
      <div class="form-section">
        <div class="row-between">
          <div class="section-label">Enable Feedback <span class="required">*</span></div>
          <el-switch v-model="formData.enableFeedback" />
        </div>
      </div>

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { 
  EditPen, FullScreen, Key
} from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  node: any
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const nodeData = ref<any>(null)

// Form Data
const formData = ref({
  inputPrompt: '请输入你的二次问题？',
  enableFeedback: true
})

// Sync to node data
watch(formData, (newVal) => {
  if (props.node && props.node.data && props.node.data.type === 'human') {
    props.node.data.inputPrompt = newVal.inputPrompt
    props.node.data.enableFeedback = newVal.enableFeedback
  }
}, { deep: true })

// Initialize form data
const initFormData = () => {
  if (props.node && props.node.data) {
    const data = props.node.data
    formData.value.inputPrompt = data.inputPrompt || '请输入你的二次问题？'
    formData.value.enableFeedback = data.enableFeedback !== undefined ? data.enableFeedback : true
  }
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    initFormData()
  }
})

watch(() => props.node, (val) => {
  if (val && val.data?.type === 'human') {
    nodeData.value = val
    initFormData()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = (done: () => void) => {
  done()
}
</script>

<style scoped>
.drawer-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drawer-title {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.drawer-content {
  padding: 10px;
}

.form-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.required {
  color: #ef4444;
}

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-tools {
  display: flex;
  gap: 8px;
  color: #6b7280;
  cursor: pointer;
}

/* Custom Input Styling to match Mui a bit */
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #374151 inset;
}
</style>
