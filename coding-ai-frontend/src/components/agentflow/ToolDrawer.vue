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
           <el-select 
             v-model="formData.tool" 
             placeholder="Select Tool" 
             style="width: 100%"
             @change="handleToolChange"
           >
             <template #prefix>
               <el-icon><Tools /></el-icon>
             </template>
             <el-option 
               v-for="t in mcpTools" 
               :key="t.name" 
               :label="t.name" 
               :value="t.name" 
             />
           </el-select>
        </div>
      </div>

      <!-- Tool Parameters (Dynamic based on tool_params) -->
      <div class="form-section" v-if="selectedToolParams.length > 0">
         <el-collapse v-model="activeCollapse">
           <el-collapse-item :title="(formData.tool || 'Tool') + ' Parameters'" name="1">
              <div v-for="param in selectedToolParams" :key="param.key" class="param-item">
                 <div class="section-label">
                   {{ param.desc || param.key }}
                   <span class="required" v-if="true">*</span>
                   <el-tooltip content="支持引用变量" placement="top">
                     <span class="variable-hint" v-pre>{{x}}</span>
                   </el-tooltip>
                 </div>
                 <VariableInput 
                   v-model="formData.params[param.key]" 
                   :variables="availableVariables"
                   class="tool-param-input"
                 />
              </div>
           </el-collapse-item>
         </el-collapse>
      </div>


    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { 
  EditPen, Tools
} from '@element-plus/icons-vue'
import { AgentFlowAPI, type ToolInfo } from '@/api/agentFlow'
import VariableInput from '@/components/common/VariableInput.vue'
import { useGraphVariables } from '@/composables/useGraphVariables'

const props = defineProps<{
  modelValue: boolean
  node: any
}>()

// 获取当前节点可用的变量
const currentNodeId = computed(() => props.node?.id || '')
const { availableVariables } = useGraphVariables(currentNodeId)

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const nodeData = ref<any>(null)
const mcpTools = ref<ToolInfo[]>([])
const activeCollapse = ref(['1'])

// Form Data
const formData = ref({
  tool: '',
  params: {} as Record<string, any>
})

const selectedToolParams = computed(() => {
  const tool = mcpTools.value.find(t => t.name === formData.value.tool)
  if (!tool || !tool.params) return []
  return Object.entries(tool.params).map(([key, desc]) => ({ key, desc }))
})

// Load Tools
const loadMcpTools = async () => {
  try {
    const response = await AgentFlowAPI.getTools()
    if ((response.code === 1 || response.success) && response.data) {
      mcpTools.value = response.data.filter((t: any) => t && t.name)
    }
  } catch (error) {
    console.error('Failed to load tools:', error)
  }
}

const handleToolChange = () => {
  // Reset params when tool changes, or maybe keep common ones? 
  // For now, let's keep it simple and just ensure reactivity works
  // We might want to clear params: formData.value.params = {}
}

// Watchers
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    initFormData()
    // Ensure tools are loaded when drawer opens
    if (mcpTools.value.length === 0) {
        loadMcpTools()
    }
  }
})

watch(() => props.node, (val) => {
  if (val && val.data?.type === 'tool') {
    nodeData.value = val
    initFormData()
  }
})

watch(formData, (newVal) => {
  if (props.node && props.node.data && props.node.data.type === 'tool') {
    props.node.data.tool = newVal.tool
    props.node.data.params = JSON.parse(JSON.stringify(newVal.params))
  }
}, { deep: true })

const initFormData = () => {
  if (props.node && props.node.data) {
    const data = props.node.data
    formData.value.tool = data.tool || ''
    formData.value.params = data.params ? JSON.parse(JSON.stringify(data.params)) : {}
  }
}

watch(visible, (val) => {
  emit('update:modelValue', val)
})

onMounted(() => {
  loadMcpTools()
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
  justify-content: space-between;
}

.required {
  color: #ef4444;
}

.add-btn {
  width: 100%;
  border-style: dashed;
}

.param-item {
  margin-bottom: 12px;
}

/* Custom Input Styling to match Mui a bit */
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #374151 inset;
}

/* 变量引用标识 */
.variable-hint {
  color: #409eff;
  font-size: 12px;
  margin-left: 8px;
  cursor: help;
}

/* Variable Input 样式调整 */
.tool-param-input :deep(.variable-editor) {
  min-height: 36px;
  padding: 6px 12px;
}
</style>
