<template>
  <el-drawer
    v-model="visible"
    :title="nodeData?.label || 'Tool Configuration'"
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
      <!-- Tool Selection -->
      <div class="form-section">
        <div class="section-label">Tool <span class="required">*</span></div>
        <div class="tool-select-wrapper">
           <el-select v-model="formData.tool" placeholder="Select Tool" style="width: 100%">
             <!-- 这里将来可以从后端加载工具列表 -->
             <el-option label="Google Search" value="google_search" />
             <el-option label="Calculator" value="calculator" />
             <el-option label="Weather" value="weather" />
             <el-option label="GitHub API" value="github_api" />
           </el-select>
        </div>
      </div>

      <!-- Update Flow State -->
       <div class="form-section">
        <div class="section-label">Update Flow State <el-icon><InfoFilled /></el-icon></div>
        <el-button class="add-btn" plain type="primary">
          <el-icon><Plus /></el-icon> Add Update Flow State
        </el-button>
      </div>

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { 
  EditPen, Plus, InfoFilled
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
  tool: ''
})

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(() => props.node, (val) => {
  if (val) {
    nodeData.value = val
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

.add-btn {
  width: 100%;
  border-style: dashed;
}

/* Custom Input Styling to match Mui a bit */
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #374151 inset;
}
</style>
