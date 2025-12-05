<template>
  <el-drawer
    v-model="visible"
    :title="nodeData?.label || 'Condition Configuration'"
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
      <!-- Conditions List -->
      <div class="form-section">
        <div class="section-header">
          <div class="section-label">Conditions <span class="required">*</span></div>
        </div>
        
        <div class="condition-card" v-for="(condition, index) in formData.conditions" :key="index">
          <div class="card-header">
            <!-- 逻辑标签: 第一个是 IF，之后是 ELSE IF -->
            <el-tag size="small" :type="index === 0 ? 'primary' : 'warning'">
              {{ index === 0 ? 'IF' : 'ELSE IF' }}
            </el-tag>
            <el-button 
              v-if="index > 0" 
              type="danger" 
              link 
              icon="Delete" 
              class="delete-btn" 
              @click="removeCondition(index)" 
            />
          </div>
          
          <!-- Type -->
          <div class="param-item">
             <div class="section-label">Type <span class="required">*</span></div>
             <el-select v-model="condition.type" style="width: 100%">
               <el-option label="String" value="string" />
               <el-option label="Number" value="number" />
               <el-option label="Boolean" value="boolean" />
             </el-select>
          </div>

          <!-- Value 1 -->
          <div class="param-item">
             <div class="section-label">Value 1 <span class="required">*</span></div>
             <el-input v-model="condition.value1" placeholder="Value 1" />
          </div>

          <!-- Operation -->
          <div class="param-item">
             <div class="section-label">Operation <span class="required">*</span></div>
             <el-select v-model="condition.operation" style="width: 100%">
               <el-option label="Equal" value="equal" />
               <el-option label="Not Equal" value="not_equal" />
               <el-option label="Contains" value="contains" />
             </el-select>
          </div>

          <!-- Value 2 -->
          <div class="param-item">
             <div class="section-label">Value 2 <span class="required">*</span></div>
             <el-input v-model="condition.value2" placeholder="Value 2" />
          </div>
        </div>

        <!-- Add Button -->
        <el-button class="add-btn" plain type="primary" @click="addCondition">
          <el-icon><Plus /></el-icon> Add ELSE IF
        </el-button>
      </div>
      
      <!-- ELSE Branch Info -->
      <div class="form-section else-section">
        <el-alert
          title="Default Branch (ELSE)"
          type="info"
          description="If none of the above conditions are met, the flow will proceed to the ELSE branch."
          show-icon
          :closable="false"
        />
      </div>

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { 
  EditPen, Plus, Delete
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
  conditions: [
    { type: 'string', value1: '', operation: 'equal', value2: '' } // Initial IF
  ]
})

// 监听 conditions 变化，实时更新节点 outputs
watch(() => formData.value.conditions, (newConditions) => {
  if (props.node && props.node.data) {
    const outputs = []
    
    // 1. IF (始终存在，对应 index 0)
    if (newConditions.length > 0) {
        outputs.push({ id: 'if', label: 'IF', labelClass: 'text-blue' })
    }
    
    // 2. ELSE IF (对应 index 1+)
    for (let i = 1; i < newConditions.length; i++) {
        outputs.push({ 
            id: `else-if-${i}`, 
            label: 'ELSE IF', 
            labelClass: 'text-blue' 
        })
    }
    
    // 3. ELSE (始终存在)
    outputs.push({ id: 'else', label: 'ELSE', labelClass: 'text-orange' })
    
    // 更新节点数据
    props.node.data.outputs = outputs
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

const addCondition = () => {
  formData.value.conditions.push({ 
    type: 'string', 
    value1: '', 
    operation: 'equal', 
    value2: '' 
  })
}

const removeCondition = (index: number) => {
  formData.value.conditions.splice(index, 1)
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

.param-item {
  margin-bottom: 12px;
}

/* Condition Card */
.condition-card {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  position: relative;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
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
