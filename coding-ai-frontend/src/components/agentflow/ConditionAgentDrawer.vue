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

      <!-- Instructions Section -->
      <div class="form-section">
        <div class="section-label row-between">
          <span>Instructions <span class="required">*</span></span>
          <div class="content-tools">
             <el-icon><InfoFilled /></el-icon>
             <el-icon><FullScreen /></el-icon>
          </div>
        </div>
        <el-input 
          v-model="formData.instructions" 
          type="textarea" 
          :rows="6" 
          resize="none"
          placeholder="Enter instructions..."
        />
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
             <el-input v-model="scenario.value" placeholder="Enter scenario..." />
          </div>
        </div>

        <el-button class="add-btn" plain type="primary" @click="addScenario">
          <el-icon><Plus /></el-icon> Add Scenarios
        </el-button>
      </div>

      <!-- Override System Prompt -->
      <div class="form-section">
        <div class="row-between">
          <div class="section-label">Override System Prompt <el-icon><InfoFilled /></el-icon></div>
          <el-switch v-model="formData.overrideSystemPrompt" />
        </div>
      </div>
      
       <div class="form-section" v-if="formData.overrideSystemPrompt">
        <div class="section-label row-between">
          <span>Node System Prompt</span>
          <div class="content-tools">
             <el-icon><InfoFilled /></el-icon>
             <el-icon><FullScreen /></el-icon>
          </div>
        </div>
        <el-input 
          v-model="formData.systemPrompt" 
          type="textarea" 
          :rows="8" 
          resize="none"
        />
      </div>

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { 
  EditPen, Cpu, ArrowDown, Setting, Edit, Plus, Delete, 
  MagicStick, FullScreen, InfoFilled 
} from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  node: any
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const nodeData = ref<any>(null)
const activeNames = ref(['1'])

// Form Data
const formData = ref({
  model: 'ChatAlibabaTongyi',
  credential: 'alibaba_apikey',
  modelName: 'qwen-plus',
  temperature: 0.9,
  streaming: true,
  instructions: `你是一个意图分析大师，能够根据用户的提问，选择最佳路由模型。

agent：能够与用户进行对话问答，且能够执行查询github仓库。
chat：仅能够和用户进行对话。

任务，请你根据用户输入选择合适的场景。你只能返回 'agent' 或者 'chat'。`,
  input: '{{ question }}',
  scenarios: [
    { value: 'agent' },
    { value: 'chat' }
  ],
  overrideSystemPrompt: true,
  systemPrompt: `You are part of a multi-agent system designed to make agent coordination and execution easy. Your task is to analyze the given input and select one matching scenario from a provided set of scenarios.

- Input: A string representing the user's query, message or data.
- Scenarios: A list of predefined scenarios that relate to the input.
- Instruction: Determine which of the provided scenarios is the best fit for the input.

Steps
1. Read the input string and the list of scenarios.
2. Analyze the content of the input to identify its main topic or intention.
3. Compare the input with each scenario: Evaluate how well the input's topic or intention aligns with each of the provided scenarios and select the one that is the best fit.
4. Output the result: Return the selected scenario in the specified JSON format.`
})

// 监听 scenarios 变化，实时更新节点 outputs
watch(() => formData.value.scenarios, (newScenarios) => {
  if (props.node && props.node.data) {
    // 生成新的 outputs
    const newOutputs = newScenarios.map((scenario, index) => ({
      id: String(index), 
      label: scenario.value, // 显示场景值
      labelClass: '' 
    }))
    
    // 更新节点数据
    props.node.data.outputs = newOutputs
  }
}, { deep: true })

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

const addScenario = () => {
  formData.value.scenarios.push({ value: '' })
}

const removeScenario = (index: number) => {
  formData.value.scenarios.splice(index, 1)
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
