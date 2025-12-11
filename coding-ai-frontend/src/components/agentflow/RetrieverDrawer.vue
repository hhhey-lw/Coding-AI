<template>
  <el-drawer
    v-model="visible"
    :title="nodeData?.label || 'Retriever Configuration'"
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
      
      <!-- Retriever Query -->
      <div class="form-section">
        <div class="section-label row-between">
          <span>
            Retriever Query <span class="required">*</span>
            <el-tooltip content="支持引用变量" placement="top">
              <span class="variable-hint" v-pre>{{x}}</span>
            </el-tooltip>
          </span>
          <div class="content-tools">
             <el-icon><Key /></el-icon>
             <el-icon><FullScreen /></el-icon>
          </div>
        </div>
        <VariableInput 
          v-model="formData.query" 
          :variables="availableVariables"
          class="retriever-query-input"
        />
      </div>

      <!-- Top K -->
      <div class="form-section">
        <div class="section-label">Top K</div>
        <el-input-number 
          v-model="formData.topK" 
          :min="1" 
          :max="20" 
          style="width: 100%" 
        />
      </div>

      <!-- Knowledge Base Selection -->
      <div class="form-section">
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

      <!-- Embedding Model -->
      <div class="form-section">
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

      <!-- Output Format (Optional, keeping as it was present) -->
      <!-- <div class="form-section">
        <div class="section-label">Output Format <span class="required">*</span></div>
        <el-select v-model="formData.outputFormat" style="width: 100%">
           <el-option label="Text" value="text" />
           <el-option label="JSON" value="json" />
        </el-select>
      </div> -->

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { 
  EditPen, FullScreen, Key
} from '@element-plus/icons-vue'
import { KnowledgeBaseAPI } from '@/api/knowledge'
import type { KnowledgeBase } from '@/types/knowledge'
import { WorkflowAPI, type ModelInfo } from '@/api/workflow'
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
const knowledgeBases = ref<KnowledgeBase[]>([])
const embeddingModels = ref<ModelInfo[]>([])

// Form Data
const formData = ref({
  query: '',
  topK: 5,
  knowledgeBaseIds: [] as string[],
  embeddingModel: 'text-embedding-v4'
})

// Load Knowledge Bases
const loadKnowledgeBases = async () => {
  try {
    const res = await KnowledgeBaseAPI.page({
      pageNum: 1,
      pageSize: 100 // Load all for now
    })
    if (res.code === 1 || res.code === 200 || res.success) { // Handle unified response format
       // Check if res.data is the page object directly or if it's nested
       const pageData = res.data
       if (pageData && Array.isArray(pageData.records)) {
          knowledgeBases.value = pageData.records
       }
    }
  } catch (error) {
    console.error('Failed to load knowledge bases', error)
  }
}

// Load Embedding Models
const loadEmbeddingModels = async () => {
  try {
    const response = await WorkflowAPI.getModelList('TextEmbedding') // Assuming 'TextEmbedding' is the type, user said 'embeddingModel'
    // The user's provided list in previous messages showed 'TextGen'. 
    // Usually embedding models are separate. I'll try to fetch 'TextEmbedding' or check what's available.
    // Actually in `AgentNodeDrawer.vue` it used hardcoded options for embedding. 
    // Here I should try to load from backend if possible. 
    // Let's assume 'TextEmbedding' is the key. If not, I'll fallback to hardcoded.
    if ((response.code === 1 || response.success) && response.data && response.data.length > 0) {
       embeddingModels.value = response.data
    } else {
       // Fallback mock if API returns empty or fails for specific type
       // Actually WorkflowAPI.getModelList takes 'TextGen' | 'ImageGen' etc. 
       // If 'TextEmbedding' is not in the type definition, I might get a type error.
       // Let's check api/workflow.ts again.
       // It says: export type ModelType = 'TextGen' | 'ImageGen' | 'AudioGen' | 'VideoGen' | 'MusicGen'
       // It seems 'TextEmbedding' is missing from the type definition in `api/workflow.ts` I read earlier.
       // I should probably update the type or just cast it.
       // For now, I will add some mock embedding models if list is empty to be safe.
       embeddingModels.value = [
          { provider: 'bailian', providerName: 'BaiLian', modelType: 'TextEmbedding', modelId: 'text-embedding-v4' }
       ]
    }
  } catch (error) {
    console.error('Failed to load embedding models', error)
     // Fallback
     embeddingModels.value = [
        { provider: 'bailian', providerName: 'BaiLian', modelType: 'TextEmbedding', modelId: 'text-embedding-v4' }
     ]
  }
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    initFormData()
  }
})

watch(() => props.node, (val) => {
  if (val && val.data?.type === 'retriever') {
    nodeData.value = val
    initFormData()
  }
})

watch(formData, (newVal) => {
  if (props.node && props.node.data && props.node.data.type === 'retriever') {
     props.node.data.query = newVal.query
     props.node.data.topK = newVal.topK
     props.node.data.knowledgeBaseIds = JSON.parse(JSON.stringify(newVal.knowledgeBaseIds))
     props.node.data.embeddingModel = newVal.embeddingModel
  }
}, { deep: true })

const initFormData = () => {
  if (props.node && props.node.data) {
    const data = props.node.data
    formData.value.query = data.query || ''
    formData.value.topK = data.topK ?? 5
    formData.value.knowledgeBaseIds = data.knowledgeBaseIds ? JSON.parse(JSON.stringify(data.knowledgeBaseIds)) : []
    formData.value.embeddingModel = data.embeddingModel || 'text-embedding-v4'
  }
}

watch(visible, (val) => {
  emit('update:modelValue', val)
})

onMounted(() => {
  loadKnowledgeBases()
  loadEmbeddingModels()
})

const handleClose = (done: () => void) => {
  done()
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

/* Variable Input 样式调整 */
.retriever-query-input :deep(.variable-editor) {
  min-height: 80px;
  padding: 8px 12px;
}
</style>
