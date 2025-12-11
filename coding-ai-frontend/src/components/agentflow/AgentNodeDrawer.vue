<template>
  <el-drawer
    v-model="visible"
    :title="nodeData?.label || 'Agent Configuration'"
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

      <!-- Parameters Section -->
      <div class="form-section">
        <div class="section-label">
          <el-icon><Setting /></el-icon>
          <span style="margin-left: 4px">{{ formData.modelName || 'Model' }} Parameters</span>
        </div>
        
        <div class="parameters-card">
          <div class="param-item">
             <div class="section-label">Temperature</div>
             <el-input-number v-model="formData.temperature" :step="0.1" :min="0" :max="1" style="width: 100%" />
          </div>
        </div>
      </div>

      <!-- Messages Section -->
      <div class="form-section">
        <div class="section-header">
          <div class="section-label">Messages</div>
        </div>
        
        <div class="message-card" v-for="(msg, index) in formData.messages" :key="index">
          <div class="card-header">
            <el-tag size="small" type="info">{{ index }}</el-tag>
            <el-button type="danger" link icon="Delete" class="delete-btn" @click="removeMessage(index)" />
          </div>
          
          <div class="param-item">
             <div class="section-label">Role <span class="required">*</span></div>
             <el-select v-model="msg.role" style="width: 100%">
               <el-option label="System" value="system" />
               <el-option label="User" value="user" />
               <el-option label="Assistant" value="assistant" />
             </el-select>
          </div>

          <div class="param-item">
             <div class="section-label">
                <span>
                  Content <span class="required">*</span>
                  <el-tooltip content="支持引用变量" placement="top">
                    <span class="variable-hint" v-pre>{{x}}</span>
                  </el-tooltip>
                </span>
             </div>
             <VariableInput 
               v-model="msg.content" 
               :variables="availableVariables"
             />
          </div>
        </div>

        <el-button class="add-btn" plain type="primary" @click="addMessage">
          <el-icon><Plus /></el-icon> Add Messages
        </el-button>
      </div>

      <!-- Tools Section (Only for Agent) -->
      <div class="form-section" v-if="!isLlmNode">
        <div class="section-header">
           <div class="section-label">Tools</div>
        </div>
        
        <!-- Existing Tools List -->
        <div class="tool-card" v-for="(tool, index) in formData.tools" :key="index">
           <div class="card-header">
            <el-tag size="small" type="info">{{ index }}</el-tag>
            <el-button type="danger" link icon="Delete" class="delete-btn" @click="removeTool(index)" />
          </div>
           <div class="param-item">
             <div class="section-label">Tool <span class="required">*</span></div>
             <el-select v-model="tool.name" placeholder="Select Tool" style="width: 100%">
                <template #prefix>
                   <el-icon><Tools /></el-icon>
                </template>
                <el-option 
                  v-for="t in validMcpTools" 
                  :key="t.name" 
                  :label="t.name" 
                  :value="t.name" 
                />
             </el-select>
          </div>
        </div>

        <el-button class="add-btn" plain type="primary" @click="addTool">
          <el-icon><Plus /></el-icon> Add Tools
        </el-button>
      </div>

      <!-- Knowledge Base Selection -->
      <div class="form-section" v-if="!isLlmNode">
        <div class="section-header">
           <div class="section-label">
             Knowledge Base
             <el-tooltip content="Give your agent context from existing knowledge bases" placement="top">
               <el-icon class="info-icon"><InfoFilled /></el-icon>
             </el-tooltip>
           </div>
        </div>
        
        <div class="knowledge-config-card" v-if="formData.hasKnowledge">
           <div class="card-header">
            <el-tag size="small" type="info">0</el-tag>
            <el-button type="danger" link icon="Delete" class="delete-btn" @click="removeKnowledge" />
          </div>

           <div class="param-item">
             <div class="section-label">Knowledge Base <span class="required">*</span></div>
             <el-select 
               v-model="formData.knowledgeBaseIds" 
               multiple 
               placeholder="Select Knowledge Base" 
               style="width: 100%"
             >
                <el-option 
                  v-for="kb in knowledgeBases" 
                  :key="kb.id" 
                  :label="kb.name" 
                  :value="kb.id" 
                />
             </el-select>
           </div>

           <div class="param-item">
             <div class="section-label">Top K</div>
             <el-input-number 
               v-model="formData.topK" 
               :min="1" 
               :max="20" 
               style="width: 100%" 
             />
           </div>

           <div class="param-item">
             <div class="section-label">Embedding Model <span class="required">*</span></div>
             <el-select v-model="formData.embeddingModel" placeholder="Select Embedding Model" style="width: 100%">
                <el-option 
                  v-for="model in embeddingModels" 
                  :key="model.modelId" 
                  :label="model.modelId" 
                  :value="model.modelId" 
                />
             </el-select>
           </div>
        </div>

        <el-button class="add-btn" plain type="primary" @click="addKnowledge" v-else>
          <el-icon><Plus /></el-icon> Add Knowledge Base
        </el-button>
      </div>

      <!-- Memory -->
      <div class="form-section">
        <div class="row-between">
          <div class="section-label">Enable Memory <el-icon><InfoFilled /></el-icon></div>
          <el-switch v-model="formData.enableMemory" />
        </div>
      </div>

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { 
  EditPen, Cpu, Setting, Plus, 
  MagicStick, Tools, InfoFilled 
} from '@element-plus/icons-vue'
import { WorkflowAPI, type ModelInfo } from '@/api/workflow'
import { AgentFlowAPI, type ToolInfo } from '@/api/agentFlow'
import { KnowledgeBaseAPI } from '@/api/knowledge'
import type { KnowledgeBase } from '@/types/knowledge'
import { ElMessage } from 'element-plus'

import VariableInput from '@/components/common/VariableInput.vue'
import { useGraphVariables } from '@/composables/useGraphVariables'

const props = defineProps<{
  modelValue: boolean
  node: any
  nodeType?: string
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const nodeData = ref<any>(null)

const nodeId = computed(() => props.node?.id || '')
const { availableVariables } = useGraphVariables(nodeId)

// 判断节点类型
const isLlmNode = computed(() => {
  return props.nodeType === 'llm'
})

// Form Data
const formData = ref({
  provider: '',
  modelName: '',
  temperature: 0.9,
  streaming: true,
  messages: [
    { role: 'system', content: '你是一个乐于助人的AI助手。能够调用工具完成用户的任务' }
  ],
  tools: [] as any[],
  enableMemory: true,
  knowledgeBaseIds: [] as string[],
  topK: 5,
  embeddingModel: 'text-embedding-v4',
  hasKnowledge: false
})

// Model data
const allModels = ref<ModelInfo[]>([])
const mcpTools = ref<ToolInfo[]>([]) // MCP Tools list
const knowledgeBases = ref<KnowledgeBase[]>([])
const embeddingModels = ref<ModelInfo[]>([])

const validMcpTools = computed(() => {
  return mcpTools.value.filter(t => t && t.name)
})

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

const isInitializing = ref(false)

// Watchers
watch(formData, (newVal) => {
  if (isInitializing.value) return
  if (props.node && props.node.data) {
    const data = props.node.data
    data.provider = newVal.provider
    data.modelName = newVal.modelName
    data.temperature = newVal.temperature
    data.streaming = newVal.streaming
    data.enableMemory = newVal.enableMemory
    
    // Deep copy arrays
    data.messages = JSON.parse(JSON.stringify(newVal.messages))
    data.tools = JSON.parse(JSON.stringify(newVal.tools))
    data.knowledgeBaseIds = JSON.parse(JSON.stringify(newVal.knowledgeBaseIds))
    data.topK = newVal.topK
    data.embeddingModel = newVal.embeddingModel
    data.hasKnowledge = newVal.hasKnowledge
  }
}, { deep: true })

const initFormData = () => {
  if (props.node && props.node.data) {
    isInitializing.value = true
    try {
      const data = props.node.data
      formData.value.provider = data.provider || ''
      formData.value.modelName = data.modelName || ''
      formData.value.temperature = data.temperature ?? 0.9
      formData.value.streaming = data.streaming ?? true
      formData.value.enableMemory = data.enableMemory ?? true
      
      if (data.messages) {
        formData.value.messages = JSON.parse(JSON.stringify(data.messages))
      }
      
      if (data.tools) {
        formData.value.tools = JSON.parse(JSON.stringify(data.tools))
      } else {
        formData.value.tools = []
      }

      if (data.knowledgeBaseIds) {
        formData.value.knowledgeBaseIds = JSON.parse(JSON.stringify(data.knowledgeBaseIds))
      } else {
        formData.value.knowledgeBaseIds = []
      }
      formData.value.topK = data.topK ?? 5
      formData.value.embeddingModel = data.embeddingModel || 'text-embedding-v4'
      
      // Initialize hasKnowledge based on data presence if not explicitly set
      if (data.hasKnowledge !== undefined) {
         formData.value.hasKnowledge = data.hasKnowledge
      } else {
         formData.value.hasKnowledge = (data.knowledgeBaseIds && data.knowledgeBaseIds.length > 0)
      }
    } finally {
      setTimeout(() => {
        isInitializing.value = false
      }, 0)
    }
  }
}

// Load data
const loadData = async () => {
  await Promise.all([loadModels(), loadMcpTools(), loadKnowledgeBases(), loadEmbeddingModels()])
}

// Load models
const loadModels = async () => {
  try {
    const response = await WorkflowAPI.getModelList('TextGen')
    if ((response.code === 1 || response.success) && response.data) {
      allModels.value = response.data
    }
  } catch (error) {
    console.error('Failed to load models:', error)
    ElMessage.error('加载模型列表失败')
  }
}

// Load Embedding Models
const loadEmbeddingModels = async () => {
  try {
    const response = await WorkflowAPI.getModelList('TextEmbedding' as any) 
    if ((response.code === 1 || response.success) && response.data && response.data.length > 0) {
       embeddingModels.value = response.data
    } else {
       embeddingModels.value = [
          { provider: 'bailian', providerName: 'BaiLian', modelType: 'TextEmbedding', modelId: 'text-embedding-v4' }
       ]
    }
  } catch (error) {
    console.error('Failed to load embedding models', error)
     embeddingModels.value = [
        { provider: 'bailian', providerName: 'BaiLian', modelType: 'TextEmbedding', modelId: 'text-embedding-v4' }
     ]
  }
}

// Load Tools
const loadMcpTools = async () => {
  try {
    const response = await AgentFlowAPI.getTools()
    console.log('AgentNodeDrawer loaded tools:', response)
    if ((response.code === 1 || response.success) && response.data) {
      mcpTools.value = response.data.filter((t: any) => t && t.name)
      console.log('AgentNodeDrawer processed mcpTools:', mcpTools.value)
    }
  } catch (error) {
    console.error('Failed to load tools:', error)
    // ElMessage.error('加载工具列表失败')
  }
}

// Load Knowledge Bases
const loadKnowledgeBases = async () => {
  try {
    const res = await KnowledgeBaseAPI.page({
      pageNum: 1,
      pageSize: 100 // Load all for now
    })
    if (res.code === 1 || res.code === 200 || res.success) { 
       const pageData = res.data
       if (pageData && Array.isArray(pageData.records)) {
          knowledgeBases.value = pageData.records
       }
    }
  } catch (error) {
    console.error('Failed to load knowledge bases', error)
  }
}

const onProviderChange = () => {
  formData.value.modelName = ''
}

onMounted(() => {
  loadData()
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    initFormData()
    if (mcpTools.value.length === 0) {
      loadMcpTools()
    }
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

const addTool = () => {
  formData.value.tools.push({ name: '' })
}

const removeTool = (index: number) => {
  formData.value.tools.splice(index, 1)
}

const addMessage = () => {
  formData.value.messages.push({ role: 'user', content: '' })
}

const removeMessage = (index: number) => {
  formData.value.messages.splice(index, 1)
}

const addKnowledge = () => {
  formData.value.hasKnowledge = true
}

const removeKnowledge = () => {
  formData.value.hasKnowledge = false
  formData.value.knowledgeBaseIds = []
  formData.value.embeddingModel = ''
  formData.value.topK = 5
}
</script>

<style scoped>
.form-section {
  margin-bottom: 24px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-header {
  margin-bottom: 12px;
}

.required {
  color: #ef4444;
  margin-left: 4px;
}

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  font-weight: 600;
  font-size: 16px;
  color: #111827;
}

.drawer-content {
  padding: 20px;
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
  margin-top: 8px;
}

.parameters-card, .knowledge-config-card {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.message-card, .tool-card, .knowledge-card {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.param-item {
  margin-bottom: 16px;
}

.param-item:last-child {
  margin-bottom: 0;
}

.info-icon {
  margin-left: 4px;
  color: #9ca3af;
  cursor: pointer;
}

/* Custom Input Styling to match Mui a bit */
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #374151 inset;
}
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}
:deep(.el-drawer__body) {
  padding: 0;
}

/* 变量引用标识 */
.variable-hint {
  color: #409eff;
  font-size: 12px;
  margin-left: 8px;
  cursor: help;
}
</style>
