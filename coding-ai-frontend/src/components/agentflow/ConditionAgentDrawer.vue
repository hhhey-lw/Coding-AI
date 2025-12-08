<template>
  <el-drawer
    v-model="visible"
    :title="nodeData?.label || 'Condition Agent Configuration'"
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
      <!-- Model Selection - Two Tier -->
      <div class="form-section">
        <div class="section-label">
          Model <span class="required">*</span>
        </div>
        
        <!-- Provider Selection -->
        <div class="model-select-wrapper" style="margin-bottom: 12px;">
          <el-select
            v-model="formData.provider"
            placeholder="Select Provider"
            style="width: 100%"
            @change="onProviderChange"
          >
            <template #prefix>
              <el-icon><Cpu /></el-icon>
            </template>
            <el-option
              v-for="provider in providers"
              :key="provider.provider"
              :label="provider.providerName"
              :value="provider.provider"
            />
          </el-select>
        </div>
        
        <!-- Model Selection -->
        <div class="model-select-wrapper">
          <el-select
            v-model="formData.modelName"
            placeholder="Select Model"
            style="width: 100%"
            :disabled="!formData.provider"
          >
            <template #prefix>
              <el-icon><MagicStick /></el-icon>
            </template>
            <el-option
              v-for="model in models"
              :key="model.modelId"
              :label="model.modelId"
              :value="model.modelId"
            />
          </el-select>
        </div>
      </div>


      <!-- Input Section -->
      <div class="form-section">
        <div class="section-label row-between">
          <span>Input <span class="required">*</span></span>
          <div class="content-tools">
             <el-icon><InfoFilled /></el-icon>
             <el-icon><FullScreen /></el-icon>
          </div>
        </div>
        <el-input 
          v-model="formData.input" 
          type="textarea" 
          :rows="2" 
          resize="none"
        />
      </div>

      <!-- Scenarios Section -->
      <div class="form-section">
        <div class="section-header">
          <div class="section-label">Scenarios <span class="required">*</span></div>
        </div>
        
        <div class="message-card" v-for="(scenario, index) in formData.scenarios" :key="index">
          <div class="card-header">
            <el-tag size="small" type="info">{{ index }}</el-tag>
            <el-button type="danger" link icon="Delete" class="delete-btn" @click="removeScenario(index)" />
          </div>
          
          <div class="param-item">
             <div class="section-label">Scenario <span class="required">*</span></div>
             <el-input v-model="scenario.scenario" placeholder="Enter scenario..." />
          </div>
          
          <div class="param-item">
             <div class="section-label">Description</div>
             <el-input 
               v-model="scenario.description" 
               type="textarea"
               :rows="3"
               resize="none"
               placeholder="Enter scenario description..." 
             />
          </div>
        </div>

        <el-button class="add-btn" plain type="primary" @click="addScenario">
          <el-icon><Plus /></el-icon> Add Scenarios
        </el-button>
      </div>


    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { 
  EditPen, Cpu, Plus, Delete, 
  MagicStick, FullScreen, InfoFilled 
} from '@element-plus/icons-vue'
import { WorkflowAPI, type ModelInfo } from '@/api/workflow'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  node: any
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const nodeData = ref<any>(null)

// Model data
const allModels = ref<ModelInfo[]>([])
const providers = computed(() => {
  const uniqueProviders = new Map<string, ModelInfo>()
  allModels.value.forEach(model => {
    if (!uniqueProviders.has(model.provider)) {
      uniqueProviders.set(model.provider, model)
    }
  })
  return Array.from(uniqueProviders.values())
})

const models = computed(() => {
  if (!formData.value.provider) return []
  return allModels.value.filter(model => model.provider === formData.value.provider)
})

// Form Data
const formData = ref({
  provider: '',
  modelName: '',
  input: '{{ question }}',
  scenarios: [
    { scenario: 'agent', description: '能够与用户进行对话问答，且能够执行查询github仓库' },
    { scenario: 'chat', description: '仅能够和用户进行对话' }
  ]
})

// 深度监听 formData 变化，同步回 node.data
const isInitializing = ref(false)
watch(formData, (newVal) => {
  if (isInitializing.value) return
  if (props.node && props.node.data) {
    const data = props.node.data
    
    // 同步基础字段
    data.provider = newVal.provider
    data.modelName = newVal.modelName
    data.input = newVal.input
    
    // 同步 scenarios 和 sceneDescriptions
    data.scenarios = newVal.scenarios.map(s => s.scenario)
    data.sceneDescriptions = newVal.scenarios.map(s => s.description)
    
    // 更新 outputs
    data.outputs = newVal.scenarios.map((scenario, index) => ({
      id: String(index), 
      label: scenario.scenario, // 显示场景值
      labelClass: '' 
    }))
  }
}, { deep: true })

// 初始化表单数据
const initFormData = () => {
  if (props.node && props.node.data) {
    isInitializing.value = true
    try {
      const data = props.node.data
      
      // 严格绑定字段
      formData.value.provider = data.provider || ''
      formData.value.modelName = data.modelName || ''
      formData.value.input = data.input || '{{ question }}'
      
      // 恢复 scenarios 结构
      if (data.scenarios && Array.isArray(data.scenarios)) {
        const descriptions = data.sceneDescriptions || []
        formData.value.scenarios = data.scenarios.map((scenario: string, index: number) => ({
          scenario: scenario,
          description: descriptions[index] || ''
        }))
      }
    } finally {
      // 确保在下一个 tick 开启监听
      setTimeout(() => {
        isInitializing.value = false
      }, 0)
    }
  }
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    initFormData()
  }
})

watch(() => props.node, (val) => {
  if (val) {
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

const addScenario = () => {
  formData.value.scenarios.push({ scenario: '', description: '' })
}

const removeScenario = (index: number) => {
  formData.value.scenarios.splice(index, 1)
}

// Load models on mount
const loadModels = async () => {
  try {
    const response = await WorkflowAPI.getModelList('TextGen')
    // 兼容后端返回结构，使用 code === 1 判断成功，或者直接判断 data 存在
    if ((response.code === 1 || response.success) && response.data) {
      allModels.value = response.data
    }
  } catch (error) {
    console.error('Failed to load models:', error)
    ElMessage.error('加载模型列表失败')
  }
}

// Handle provider change
const onProviderChange = () => {
  // Reset model selection when provider changes
  formData.value.modelName = ''
}

onMounted(() => {
  loadModels()
})
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

.param-item {
  margin-bottom: 16px;
}

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Message Card */
.message-card {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.content-tools {
  display: flex;
  gap: 8px;
  color: #6b7280;
  cursor: pointer;
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
