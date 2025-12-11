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
      <!-- Branches List -->
      <div class="form-section">
        <div class="section-header">
          <div class="section-label">Branches <span class="required">*</span></div>
        </div>
        
        <div class="condition-card" v-for="(branch, index) in formData.branches" :key="index">
          <div class="card-header">
            <!-- 逻辑标签: 第一个是 IF，之后是 ELSE IF -->
            <el-tag size="small" :type="index === 0 ? 'primary' : 'warning'">
              {{ branch.label }}
            </el-tag>
            <el-button 
              v-if="index > 0" 
              type="danger" 
              link 
              icon="Delete" 
              class="delete-btn" 
              @click="removeBranch(index)" 
            />
          </div>
          
          <!-- Conditions inside Branch -->
          <!-- Currently simplfied to 1 condition per branch for UI consistency, but data structure supports list -->
          <div v-if="branch.conditions && branch.conditions.length > 0">
             <!-- Left Value (Variable) -->
              <div class="param-item">
                 <div class="section-label">
                   Variable (Left) <span class="required">*</span>
                   <el-tooltip content="支持引用变量" placement="top">
                     <span class="variable-hint" v-pre>{{x}}</span>
                   </el-tooltip>
                 </div>
                 <VariableInput 
                   v-model="branch.conditions[0].leftValue" 
                   :variables="availableVariables"
                   class="condition-variable-input"
                 />
              </div>

              <!-- Operator -->
              <div class="param-item">
                 <div class="section-label">Operator <span class="required">*</span></div>
                 <el-select v-model="branch.conditions[0].operator" style="width: 100%" @change="onOperatorChange(branch.conditions[0])">
                   <el-option label="为空" value="IS_EMPTY" />
                   <el-option label="不为空" value="NOT_EMPTY" />
                   <el-option label="等于" value="EQUALS" />
                   <el-option label="不等于" value="NOT_EQUALS" />
                 </el-select>
              </div>

              <!-- Right Value (仅当操作符需要右值时显示) -->
              <div class="param-item" v-if="!isUnaryOperator(branch.conditions[0].operator)">
                 <div class="section-label">
                   Value (Right) <span class="required">*</span>
                   <el-tooltip content="支持引用变量" placement="top">
                     <span class="variable-hint" v-pre>{{x}}</span>
                   </el-tooltip>
                 </div>
                 <VariableInput 
                   v-model="branch.conditions[0].rightValue" 
                   :variables="availableVariables"
                   class="condition-variable-input"
                 />
              </div>
              
              <!-- Right Type (仅当操作符需要右值时显示) -->
               <div class="param-item" v-if="!isUnaryOperator(branch.conditions[0].operator)">
                 <div class="section-label">Value Type</div>
                 <el-select v-model="branch.conditions[0].rightType" style="width: 100%">
                   <el-option label="String" value="STRING" />
                   <el-option label="Number" value="NUMBER" />
                   <el-option label="Boolean" value="BOOLEAN" />
                   <el-option label="Reference" value="REF" />
                 </el-select>
              </div>
          </div>
        </div>

        <!-- Add Button -->
        <el-button class="add-btn" plain type="primary" @click="addBranch">
          <el-icon><Plus /></el-icon> Add ELSE IF
        </el-button>
      </div>
      
      <!-- ELSE Branch Info -->
      <div class="form-section else-section">
        <el-alert
          title="Default Branch (ELSE)"
          type="info"
          description="如果以上条件都不满足，将执行该默认分支。"
          show-icon
          :closable="false"
        />
      </div>

    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { 
  EditPen, Plus, Delete
} from '@element-plus/icons-vue'
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

// Form Data matches Branch.java structure
const formData = ref({
  branches: [
    { 
      id: 'if', 
      label: 'IF', 
      conditionLogic: 'AND',
      conditions: [
        { leftValue: '', leftType: 'REF', operator: 'EQUALS', rightValue: '', rightType: 'STRING' }
      ]
    } 
  ]
})

// 判断是否为一元操作符（不需要右值）
const isUnaryOperator = (operator: string) => {
  return operator === 'IS_EMPTY' || operator === 'NOT_EMPTY'
}

// 操作符变更时清空右值（如果是一元操作符）
const onOperatorChange = (condition: any) => {
  if (isUnaryOperator(condition.operator)) {
    condition.rightValue = ''
  }
}

// 监听 branches 变化，实时更新节点 outputs 和 data.branches
watch(() => formData.value.branches, (newBranches) => {
  if (props.node && props.node.data && props.node.data.type === 'condition-basic') {
    // Sync to node.data.branches for backend export
    props.node.data.branches = JSON.parse(JSON.stringify(newBranches))

    const outputs = []
    
    // 1. Map branches to outputs
    newBranches.forEach((branch, index) => {
        outputs.push({ 
            id: branch.id || (index === 0 ? 'if' : `else-if-${index}`),
            label: branch.label, 
            labelClass: 'text-blue' 
        })
    })
    
    // 2. ELSE (始终存在)
    outputs.push({ id: 'else', label: 'ELSE', labelClass: 'text-orange' })
    
    // 更新节点 outputs
    props.node.data.outputs = outputs
  }
}, { deep: true })

// Initialize data from node
const initFormData = () => {
    if (props.node && props.node.data) {
        if (props.node.data.branches) {
            formData.value.branches = JSON.parse(JSON.stringify(props.node.data.branches))
        } else {
            // Default init if empty
            formData.value.branches = [
                { 
                  id: 'if', 
                  label: 'IF', 
                  conditionLogic: 'AND',
                  conditions: [
                    { leftValue: '', leftType: 'REF', operator: 'EQUALS', rightValue: '', rightType: 'STRING' }
                  ]
                } 
            ]
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
  if (val && val.data?.type === 'condition-basic') {
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

const addBranch = () => {
  const index = formData.value.branches.length
  formData.value.branches.push({ 
    id: `else-if-${index}`,
    label: 'ELSE IF', 
    conditionLogic: 'AND',
    conditions: [
        { leftValue: '', leftType: 'REF', operator: 'EQUALS', rightValue: '', rightType: 'STRING' }
    ]
  })
}

const removeBranch = (index: number) => {
  formData.value.branches.splice(index, 1)
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

/* Variable Input 样式调整 */
.condition-variable-input :deep(.variable-editor) {
  min-height: 36px;
  padding: 6px 12px;
}

/* 变量引用标识 */
.variable-hint {
  color: #409eff;
  font-size: 12px;
  margin-left: 8px;
  cursor: help;
}
</style>
