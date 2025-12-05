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
      <!-- Model Selection -->
      <div class="form-section">
        <div class="section-label">
          Model <span class="required">*</span>
        </div>
        <div class="model-select-wrapper">
           <el-input
            v-model="formData.model"
            placeholder="Select Model"
            class="model-input"
          >
            <template #prefix>
              <div class="model-icon-prefix">
                 <el-icon><Cpu /></el-icon>
              </div>
            </template>
            <template #suffix>
               <el-icon class="el-input__icon"><ArrowDown /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <!-- Parameters Accordion -->
      <el-collapse v-model="activeNames" class="params-accordion">
        <el-collapse-item name="1">
          <template #title>
            <div class="accordion-title">
              <el-icon><Setting /></el-icon>
              <span>{{ formData.model }} Parameters</span>
            </div>
          </template>
          
          <div class="param-item">
            <div class="section-label">Connect Credential <span class="required">*</span></div>
            <div class="credential-row">
              <el-select v-model="formData.credential" placeholder="Select Credential" style="width: 100%">
                <el-option label="alibaba apikey" value="alibaba_apikey" />
              </el-select>
              <el-button icon="Edit" circle text />
            </div>
          </div>

          <div class="param-item">
             <div class="section-label">Model Name <span class="required">*</span></div>
             <el-input v-model="formData.modelName" placeholder="qwen-plus" />
          </div>

          <div class="param-item">
             <div class="section-label">Temperature</div>
             <el-input-number v-model="formData.temperature" :step="0.1" :min="0" :max="1" style="width: 100%" />
          </div>

           <div class="param-item row-between">
             <div class="section-label">Streaming</div>
             <el-switch v-model="formData.streaming" />
          </div>
        </el-collapse-item>
      </el-collapse>

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
             <div class="section-label row-between">
                <span>Content <span class="required">*</span></span>
                <div class="content-tools">
                  <el-icon><MagicStick /></el-icon>
                  <el-icon><FullScreen /></el-icon>
                </div>
             </div>
             <el-input 
               v-model="msg.content" 
               type="textarea" 
               :rows="4" 
               resize="none"
               placeholder="Enter message content..."
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
        
        <!-- Existing Tools List would go here -->
        <div class="tool-card" v-if="formData.tools.length > 0">
           <div class="card-header">
            <el-tag size="small" type="info">0</el-tag>
            <el-button type="danger" link icon="Delete" class="delete-btn" />
          </div>
           <div class="param-item">
             <div class="section-label">Tool <span class="required">*</span></div>
             <el-input v-model="formData.tools[0].name" readonly>
                <template #prefix>
                   <el-icon><Tools /></el-icon>
                </template>
             </el-input>
          </div>
          <!-- More tool params... -->
        </div>

        <el-button class="add-btn" plain type="primary">
          <el-icon><Plus /></el-icon> Add Tools
        </el-button>
      </div>

      <!-- Knowledge Buttons -->
      <div class="form-section" v-if="!isLlmNode">
        <div class="section-label">Knowledge (Document Stores) <el-icon><InfoFilled /></el-icon></div>
        <el-button class="add-btn" plain type="primary">
          <el-icon><Plus /></el-icon> Add Knowledge (Document Stores)
        </el-button>
      </div>

       <div class="form-section" v-if="!isLlmNode">
        <div class="section-label">Knowledge (Vector Embeddings) <el-icon><InfoFilled /></el-icon></div>
        <el-button class="add-btn" plain type="primary">
          <el-icon><Plus /></el-icon> Add Knowledge (Vector Embeddings)
        </el-button>
      </div>

      <!-- Memory -->
      <div class="form-section">
        <div class="row-between">
          <div class="section-label">Enable Memory <el-icon><InfoFilled /></el-icon></div>
          <el-switch v-model="formData.enableMemory" />
        </div>
      </div>

       <div class="form-section" v-if="formData.enableMemory">
          <div class="section-label">Memory Type</div>
          <el-select v-model="formData.memoryType" style="width: 100%">
             <el-option label="All Messages" value="all" />
          </el-select>
       </div>

       <!-- Input Message -->
       <div class="form-section">
          <div class="section-label">Input Message <el-icon><InfoFilled /></el-icon></div>
          <el-input type="textarea" :rows="2" placeholder="" />
       </div>

       <!-- Return Response As -->
       <div class="form-section">
          <div class="section-label">Return Response As <span class="required">*</span></div>
          <el-select v-model="formData.responseType" style="width: 100%">
             <el-option label="User Message" value="user_message" />
          </el-select>
       </div>

       <!-- JSON Structured Output (Only for LLM) -->
       <div class="form-section" v-if="isLlmNode">
        <div class="section-label">JSON Structured Output <el-icon><InfoFilled /></el-icon></div>
        <el-button class="add-btn" plain type="primary">
          <el-icon><Plus /></el-icon> Add JSON Structured Output
        </el-button>
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
import { ref, watch, computed } from 'vue'
import { 
  EditPen, Cpu, ArrowDown, Setting, Edit, Plus, Delete, 
  MagicStick, FullScreen, Tools, InfoFilled, Key 
} from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  node: any
  nodeType?: string
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const nodeData = ref<any>(null)
const activeNames = ref(['1'])

// 判断节点类型
const isLlmNode = computed(() => {
  return props.nodeType === 'llm'
})

// Form Data
const formData = ref({
  model: 'ChatAlibabaTongyi',
  credential: 'alibaba_apikey',
  modelName: 'qwen-max',
  temperature: 0.9,
  streaming: true,
  messages: [
    { role: 'system', content: '你是一个乐于助人的AI助手。能够调用工具完成用户的任务' }
  ],
  tools: [{ name: 'Github MCP' }],
  enableMemory: true,
  memoryType: 'all',
  responseType: 'user_message'
})

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(() => props.node, (val) => {
  if (val) {
    nodeData.value = val
    // Here you would normally load the specific node's data
    // For now we use the default mocked formData
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = (done: () => void) => {
  done()
}

const addMessage = () => {
  formData.value.messages.push({ role: 'user', content: '' })
}

const removeMessage = (index: number) => {
  formData.value.messages.splice(index, 1)
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

.accordion-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.param-item {
  margin-bottom: 16px;
}

.credential-row {
  display: flex;
  gap: 8px;
}

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Message Card */
.message-card, .tool-card {
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

.params-accordion :deep(.el-collapse-item__header) {
  background-color: #f9fafb;
  padding: 0 12px;
  border-radius: 8px;
  margin-bottom: 4px;
}

.params-accordion :deep(.el-collapse-item__content) {
  padding: 12px;
}

/* Custom Input Styling to match Mui a bit */
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #374151 inset;
}
</style>
