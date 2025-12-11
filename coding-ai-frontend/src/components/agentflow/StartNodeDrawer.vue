<template>
  <el-drawer
    v-model="visible"
    title="Start Node Configuration"
    direction="rtl"
    size="400px"
    :before-close="handleClose"
  >
    <div class="drawer-content">
      <el-form :model="nodeData" label-position="top">
        
        <!-- Flow State Section -->
        <div class="section-title">
          <span>Flow State</span>
          <el-tooltip content="Runtime state during the execution of the workflow" placement="top">
            <el-icon><InfoFilled /></el-icon>
          </el-tooltip>
        </div>
        
        <div v-for="(item, index) in nodeData.flowState" :key="index" class="flow-state-item">
            <div class="state-header">
                <el-button 
                    v-if="item.key !== 'messages'"
                    type="danger" 
                    link 
                    :icon="Delete" 
                    @click="removeFlowState(index)"
                ></el-button>
                <el-tag v-else type="success" size="small">System</el-tag>
                <el-tag size="small" type="info">{{ index }}</el-tag>
            </div>
            
            <div class="state-fields">
                <el-form-item label="Key" required class="state-key">
                    <el-input 
                        v-model="item.key" 
                        placeholder="Foo" 
                        :disabled="item.key === 'messages'"
                    />
                </el-form-item>
                <el-form-item label="Value" class="state-value">
                    <el-input 
                        v-model="item.value" 
                        :placeholder="item.key === 'messages' ? 'Assigned from Chat' : 'Bar'" 
                        :disabled="item.key === 'messages'"
                    />
                    <div v-if="item.key === 'messages'" class="field-hint">
                        This value is automatically populated from the chat conversation.
                    </div>
                </el-form-item>
            </div>
        </div>
        
        <el-button class="add-state-btn" plain type="primary" @click="addFlowState">
            <el-icon><Plus /></el-icon> Add Flow State
        </el-button>

      </el-form>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { InfoFilled, Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'update:node'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const nodeData = ref({
    flowState: [] as {key: string, value: string}[],
    persistState: false
})

const isInitializing = ref(false)

watch(() => props.node?.id, (newId) => {
  if (props.node && props.node.data) {
    isInitializing.value = true
    // Initialize data from node
    const data = props.node.data
    const existingState = data.flowState ? JSON.parse(JSON.stringify(data.flowState)) : []

    // Ensure 'messages' key exists
    if (!existingState.find((s: any) => s.key === 'messages')) {
        existingState.unshift({ key: 'messages', value: '' })
    }

    nodeData.value = {
        flowState: existingState,
        persistState: data.persistState || false
    }
    nextTick(() => {
        isInitializing.value = false
    })
  }
}, { immediate: true })

// Watch for changes and update node data
watch(nodeData, (newVal) => {
    if (isInitializing.value) return
    if (props.node) {
        props.node.data = {
            ...props.node.data,
            flowState: newVal.flowState,
            persistState: newVal.persistState
        }
    }
}, { deep: true })

const handleClose = (done: () => void) => {
  done()
}

const addFlowState = () => {
    nodeData.value.flowState.push({ key: '', value: '' })
}

const removeFlowState = (index: number) => {
    nodeData.value.flowState.splice(index, 1)
}

</script>

<style scoped>
.drawer-content {
  padding: 0 10px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #303133;
}

.section-container {
    margin-top: 24px;
}

.section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 14px;
    color: #303133;
}

.spacer {
    flex-grow: 1;
}

.flow-state-item {
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 12px;
    margin-bottom: 12px;
    position: relative;
    background-color: #fcfcfc;
}

.state-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.add-state-btn {
    width: 100%;
    border-style: dashed;
}

:deep(.el-form-item__label) {
    font-size: 12px;
    padding-bottom: 4px;
    line-height: 1.2;
}

:deep(.el-form-item) {
    margin-bottom: 12px;
}
</style>
