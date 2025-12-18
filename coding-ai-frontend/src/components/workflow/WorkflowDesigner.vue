<template>
  <div class="workflow-designer">
    <!-- 顶部工具栏 -->
    <div class="designer-header">
      <div class="header-left">
        <el-button size="small" @click="handleBack" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2 class="workflow-title">
          {{ workflowName }}
        </h2>
      </div>
      
      <div class="header-center">
        <!-- 桌面端：显示按钮组 -->
        <el-button-group class="desktop-buttons">
          <el-button size="small" @click="handleSave">
            <el-icon><DocumentCopy /></el-icon>
            保存
          </el-button>
          <el-button size="small" @click="handleRun">
            <el-icon><VideoPlay /></el-icon>
            运行
          </el-button>
          <el-button size="small" @click="handleClear">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
          <el-button size="small" @click="handleShowRunHistory">
            <el-icon><Document /></el-icon>
            运行记录
          </el-button>
        </el-button-group>
      </div>
      
      <div class="header-right">
        <!-- 移动端：下拉菜单 -->
        <el-dropdown class="mobile-dropdown" trigger="click" @command="handleMobileCommand">
          <el-button size="small" circle>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="save">
                <el-icon><DocumentCopy /></el-icon>
                保存
              </el-dropdown-item>
              <el-dropdown-item command="run">
                <el-icon><VideoPlay /></el-icon>
                运行
              </el-dropdown-item>
              <el-dropdown-item command="clear" divided>
                <el-icon><Delete /></el-icon>
                清空
              </el-dropdown-item>
              <el-dropdown-item command="history">
                <el-icon><Document /></el-icon>
                运行记录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 白天/夜晚切换按钮已隐藏 -->
        <el-button size="small" @click="showSettings = true" circle>
          <el-icon><Setting /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="designer-content">
      <!-- 左侧节点面板 -->
      <div v-show="showNodePalette" class="node-palette-wrapper">
        <NodePalette 
          @node-select="handleNodeSelect"
          @node-drag="handleNodeDrag"
          @collapse="handleCollapseNodePalette"
        />
      </div>
      
      <!-- 移动端：添加节点按钮 -->
      <div v-if="!showNodePalette" class="mobile-add-node-btn">
        <el-button type="primary" circle @click="toggleNodePalette">
          <el-icon><DArrowRight /></el-icon>
        </el-button>
      </div>
      
      <!-- 中间工作流画布 -->
      <div class="canvas-container">
        <WorkflowEditor
          ref="workflowEditorRef"
          v-model="workflowConfig"
          @node-click="handleNodeClick"
          @edge-click="handleEdgeClick"
          @canvas-click="handleCanvasClick"
          @save="handleWorkflowSave"
          @validate="handleWorkflowValidate"
        />
      </div>
      
      <!-- 右侧节点详情侧边栏 -->
      <NodeSidebar
        v-model="showSidebar"
        :node="selectedNode"
        :available-nodes="workflowConfig.nodes"
        :edges="workflowConfig.edges"
        @node-save="handleNodeSave"
        @node-delete="handleNodeDelete"
      />
    </div>

    <!-- 设置对话框 -->
    <el-dialog v-model="showSettings" title="工作流设置" width="90%" class="settings-dialog">
      <el-form :model="settingsForm" label-width="100px">
        <el-form-item label="工作流名称">
          <el-input v-model="settingsForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="settingsForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="settingsForm.version" />
        </el-form-item>
        <!-- 网格对齐、显示网格、显示小地图选项已隐藏 -->
      </el-form>
      
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="handleSettingsSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 输入参数对话框 -->
    <el-dialog v-model="showInputParamsDialog" title="输入运行参数" width="90%" class="input-params-dialog">
      <div class="input-params-content">
        <div class="input-params-scroll">
          <div class="input-params-box">
            <el-form label-width="120px">
              <el-form-item 
                v-for="param in startNodeParams" 
                :key="param.key"
                :label="param.key"
              >
          <!-- Image 类型参数 -->
          <template v-if="param.type === 'Image'">
            <div class="image-param-row">
              <!-- 左侧：模式选择（上下排列） -->
              <div class="mode-buttons">
                <el-button 
                  size="small"
                  :type="inputParamModes[param.key] === 'upload' ? 'primary' : ''"
                  @click="inputParamModes[param.key] = 'upload'"
                >
                  本地上传
                </el-button>
                <el-button 
                  size="small"
                  :type="inputParamModes[param.key] === 'url' ? 'primary' : ''"
                  @click="inputParamModes[param.key] = 'url'"
                  style="margin-left: 0px;"
                >
                  输入URL
                </el-button>
              </div>
              
              <!-- 右侧：操作区域 -->
              <div class="mode-content">
                <!-- 本地上传模式 -->
                <template v-if="inputParamModes[param.key] === 'upload'">
                  <!-- 已上传：显示图片和删除按钮 -->
                  <div v-if="inputParams[param.key]" class="uploaded-content">
                    <el-image 
                      :src="inputParams[param.key]" 
                      fit="cover"
                      class="uploaded-image"
                    >
                      <template #error>
                        <div class="image-error-placeholder">
                          <el-icon :size="20"><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                    <el-button 
                      type="danger"
                      size="small"
                      @click="inputParams[param.key] = ''"
                    >
                      删除
                    </el-button>
                  </div>
                  
                  <!-- 未上传：显示上传按钮 -->
                  <el-upload
                    v-else
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="(file: any) => handleParamImageUpload(file, param.key)"
                    accept="image/*"
                  >
                    <el-button size="small" :loading="isUploadingParam">
                      <el-icon><Upload /></el-icon>
                      上传图片
                    </el-button>
                  </el-upload>
                </template>
                
                <!-- URL 模式 -->
                <template v-else>
                  <el-input 
                    v-model="inputParams[param.key]"
                    placeholder="请输入图片URL"
                    size="small"
                    style="padding-left: 0;"
                  />
                </template>
              </div>
            </div>
          </template>
          
          <!-- 普通类型参数 -->
          <el-input 
            v-else
            v-model="inputParams[param.key]"
            :placeholder="`请输入 ${param.key}`"
            :type="param.type === 'number' ? 'number' : 'text'"
          />
          
          <div v-if="param.desc" class="param-desc">{{ param.desc }}</div>
              </el-form-item>
              
              <el-empty v-if="startNodeParams.length === 0" description="无需输入参数" />
            </el-form>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showInputParamsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRunWithParams" :loading="isUploadingParam">开始运行</el-button>
      </template>
    </el-dialog>

    <!-- 运行结果对话框 -->
    <el-dialog v-model="showRunDialog" title="工作流运行" width="90%" class="run-result-dialog">
      <div class="run-result-content">
        <div class="run-result-scroll">
          <div class="run-dialog-content">
            <div v-if="runStatus === 'running'" class="running-status">
              <el-icon class="rotating"><LoadingIcon /></el-icon>
              <span>工作流正在运行中...</span>
            </div>
            
            <div v-else-if="runStatus === 'completed'" class="completed-status">
              <el-icon class="success-icon"><SuccessFilled /></el-icon>
              <span>工作流运行完成</span>
              
              <div class="run-results">
                <h4>工作流执行信息：</h4>
                <div v-if="runResults?.workflowInstanceVO" class="workflow-info">
                  <p><strong>执行ID:</strong> {{ runResults.workflowInstanceVO.id }}</p>
                  <p><strong>状态:</strong> {{ runResults.workflowInstanceVO.status }}</p>
                  <p><strong>开始时间:</strong> {{ runResults.workflowInstanceVO.startTime }}</p>
                  <p><strong>结束时间:</strong> {{ runResults.workflowInstanceVO.endTime }}</p>
                </div>
                
                <!-- 结束节点输出结果 -->
                <div v-if="endNodeOutput" class="end-node-output">
                  <h4>输出结果：</h4>
                  <div class="output-content scroll-x">
                    {{ endNodeOutput }}
                  </div>
                </div>
                
                <!-- 节点执行详情（可折叠） -->
                <div class="node-details-section">
                  <div class="details-header" @click="showNodeDetails = !showNodeDetails">
                    <h4>节点执行详情</h4>
                    <el-icon class="toggle-icon" :class="{ 'expanded': showNodeDetails }">
                      <ArrowRight />
                    </el-icon>
                  </div>
                  
                  <el-collapse-transition>
                    <div v-show="showNodeDetails" v-if="runResults?.workflowNodeInstanceVOList" class="node-results">
                      <div 
                        v-for="node in runResults.workflowNodeInstanceVOList" 
                        :key="node.id"
                        class="node-result-item"
                        :class="{
                          'node-success': node.status === 'success',
                          'node-executing': node.status === 'executing',
                          'node-failed': node.status === 'failed'
                        }"
                      >
                        <div class="node-header">
                          <span class="node-name">{{ node.nodeName }} ({{ node.nodeId }})</span>
                          <span class="node-status">{{ node.status }}</span>
                          <span v-if="node.executeTime" class="node-time">{{ node.executeTime }}</span>
                        </div>
                        <div v-if="node.output" class="node-output">
                          <strong>输出:</strong> {{ node.output }}
                        </div>
                        <div v-if="node.errorInfo" class="node-error">
                          <strong>错误:</strong> {{ node.errorInfo }}
                        </div>
                      </div>
                    </div>
                  </el-collapse-transition>
                </div>
              </div>
            </div>
            
            <div v-else-if="runStatus === 'error'" class="error-status">
              <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
              <span>工作流运行失败</span>
              
              <div class="error-details">
                <h4>错误信息：</h4>
                <pre class="scroll-x">{{ runError }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showRunDialog = false">关闭</el-button>
        <el-button v-if="runStatus === 'running'" type="danger" @click="handleStopRun">
          停止运行
        </el-button>
      </template>
    </el-dialog>

    <!-- 运行记录对话框 -->
    <el-dialog v-model="showRunHistoryDialog" title="工作流运行记录" width="90%" class="run-history-dialog">
      <div class="run-history-content">
        <div class="run-history-table-scroll">
          <el-table :data="runHistoryList" style="width: 100%; min-width: 860px;" v-loading="isLoadingHistory">
            <el-table-column prop="id" label="实例ID" min-width="100" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag 
                  :type="getStatusTagType(row.status)"
                  size="small"
                >
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" min-width="160" />
            <el-table-column prop="endTime" label="结束时间" min-width="160" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="handleViewRunResult(row.id)"
                >
                  查看结果
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="historyPageNum"
            v-model:page-size="historyPageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="historyTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleHistoryPageSizeChange"
            @current-change="handleHistoryPageChange"
          />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showRunHistoryDialog = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ArrowLeft, 
  DocumentCopy, 
  VideoPlay, 
  Delete, 
  Document, 
  Setting,
  MoreFilled,
  Upload,
  Picture,
  SuccessFilled,
  CircleCloseFilled,
  Loading as LoadingIcon,
  ArrowRight
} from '@element-plus/icons-vue'
import type { WorkflowConfig, WorkflowNode, WorkflowEdge, WorkflowConfigAddRequest } from '@/types/workflow'
import type { WorkflowInstanceVO } from '@/api/workflow'
import { WorkflowTransform } from '@/utils/workflowTransform'
import WorkflowAPI from '@/api/workflow'
import NodePalette from './NodePalette.vue'
import WorkflowEditor from './WorkflowEditor.vue'
import NodeSidebar from './NodeSidebar.vue'

// Router
const router = useRouter()
const route = useRoute()

// 响应式数据
const showSidebar = ref(false)
const showSettings = ref(false)
const showRunDialog = ref(false)
const showInputParamsDialog = ref(false)
const showNodePalette = ref(true)
const selectedNode = ref<WorkflowNode | null>(null)
const runStatus = ref<'idle' | 'running' | 'completed' | 'error'>('idle')
const runResults = ref<any>(null)
const runError = ref<string>('')
const workflowEditorRef = ref<any>(null)
const showNodeDetails = ref(false)
const isLoading = ref(false)
const inputParams = ref<Record<string, any>>({})
const inputParamModes = ref<Record<string, 'upload' | 'url'>>({})
const isUploadingParam = ref(false)
const showRunHistoryDialog = ref(false)
const runHistoryList = ref<WorkflowInstanceVO[]>([])
const isLoadingHistory = ref(false)
const historyPageNum = ref(1)
const historyPageSize = ref(10)
const historyTotal = ref(0)
const lastCanvasClick = ref<{ x: number; y: number } | null>(null)

// 工作流配置
const workflowConfig = ref<WorkflowConfig>({
  name: '新建工作流',
  description: '请描述您的工作流',
  version: '1.0.0',
  nodes: [],
  edges: [],
  viewport: { x: 0, y: 0, zoom: 1 },
  settings: {
    snapToGrid: true,
    gridSize: 15,
    showGrid: true,
    showMinimap: true
  }
})

// 设置表单
const settingsForm = ref({
  name: '',
  description: '',
  version: '',
  snapToGrid: true,
  showGrid: true,
  showMinimap: true
})

// 计算属性
const workflowName = computed(() => workflowConfig.value.name)

// 切换节点面板显示
const toggleNodePalette = () => {
  showNodePalette.value = !showNodePalette.value
}

const handleCollapseNodePalette = () => {
  showNodePalette.value = false
}

// 移动端下拉菜单命令处理
const handleMobileCommand = (command: string) => {
  switch (command) {
    case 'save':
      handleSave()
      break
    case 'run':
      handleRun()
      break
    case 'clear':
      handleClear()
      break
    case 'history':
      handleShowRunHistory()
      break
  }
}

// 获取开始节点的输入参数
const startNodeParams = computed(() => {
  const startNode = workflowConfig.value.nodes.find(n => n.type === 'Start')
  if (!startNode) return []
  
  const params = startNode.data.config?.input_params || []
  return params
})

// 获取结束节点的输出结果
const endNodeOutput = computed(() => {
  if (!runResults.value?.workflowNodeInstanceVOList) return null
  
  // 查找类型为 End 的节点
  const endNode = runResults.value.workflowNodeInstanceVOList.find(
    (node: any) => node.nodeType === 'End' || node.nodeName?.includes('结束')
  )
  
  return endNode?.output || null
})

// 监听工作流配置变化，同步到设置表单
watch(() => [
  workflowConfig.value.name,
  workflowConfig.value.description,
  workflowConfig.value.version,
  workflowConfig.value.settings
], ([name, description, version, settings]) => {
  settingsForm.value = {
    name: name as string,
    description: description as string || '',
    version: version as string,
    snapToGrid: (settings as any)?.snapToGrid ?? true,
    showGrid: (settings as any)?.showGrid ?? true,
    showMinimap: (settings as any)?.showMinimap ?? true
  }
}, { immediate: true })

// 监听节点变化，同步节点计数器
watch(() => workflowConfig.value.nodes, (nodes) => {
  WorkflowTransform.syncNodeCounters(nodes)
}, { immediate: true, deep: true })

// 组件挂载时，检查是否需要加载已有工作流
onMounted(() => {
  const workflowId = route.query.id as string
  if (workflowId) {
    console.log('检测到工作流ID，开始加载:', workflowId)
    loadWorkflowById(workflowId)
  } else {
    console.log('新建工作流模式')
  }

  if (window.matchMedia && window.matchMedia('(max-width: 768px)').matches) {
    showNodePalette.value = false
  }
  
  // 监听移动端添加节点事件
  document.addEventListener('node-add-mobile', handleMobileNodeAdd as EventListener)
})

// 组件卸载时清理事件监听
onUnmounted(() => {
  document.removeEventListener('node-add-mobile', handleMobileNodeAdd as EventListener)
})

// 移动端添加节点处理
const handleMobileNodeAdd = (event: CustomEvent) => {
  const nodeType = event.detail
  const editor = workflowEditorRef.value
  if (editor?.addNodeFromPaletteAtClientPoint && lastCanvasClick.value) {
    editor.addNodeFromPaletteAtClientPoint(nodeType.type, lastCanvasClick.value, nodeType)
  } else if (editor?.addNodeFromPaletteAtCenter) {
    editor.addNodeFromPaletteAtCenter(nodeType.type, nodeType)
  } else {
    handleNodeSelect(nodeType)
  }
  // 添加完后关闭节点面板
  showNodePalette.value = false
}

// 事件处理
const handleNodeSelect = (nodeType: any) => {
  console.log('Selected node type:', nodeType)
}

const handleNodeDrag = (nodeType: any) => {
  console.log('Dragging node type:', nodeType)
}

const handleNodeClick = (node: WorkflowNode) => {
  selectedNode.value = node
  showSidebar.value = true
  
  console.log('=== WorkflowDesigner: Node clicked ===')
  console.log('Selected node:', node.id, node.data.name)
  console.log('Passing edges to sidebar:', workflowConfig.value.edges)
  console.log('Total edges count:', workflowConfig.value.edges.length)
}

const handleEdgeClick = (edge: WorkflowEdge) => {
  console.log('Edge clicked:', edge)
}

const handleCanvasClick = (event?: MouseEvent) => {
  if (event) {
    lastCanvasClick.value = { x: event.clientX, y: event.clientY }
  }
  selectedNode.value = null
  showSidebar.value = false
}

const handleNodeSave = async (node: WorkflowNode) => {
  const index = workflowConfig.value.nodes.findIndex(n => n.id === node.id)
  if (index > -1) {
    // 保留原始的type、position和style，只更新data
    const originalNode = workflowConfig.value.nodes[index]
    workflowConfig.value.nodes[index] = {
      ...node,
      type: originalNode.type,  // 保留原始的节点类型
      position: originalNode.position,  // ✅ 保留原始位置，避免抖动
      style: originalNode.style,  // 保留原始样式
      data: {
        ...node.data,
        type: originalNode.data.type || originalNode.type  // 确保data.type不丢失
      }
    }
    
    console.log('节点已保存:', {
      id: node.id,
      type: originalNode.type,
      dataType: originalNode.data.type || originalNode.type,
      position: originalNode.position,
      config: node.data.config
    })
    
    // 调用接口保存整个工作流
    try {
      const request = WorkflowTransform.toBackendRequest(workflowConfig.value)
      request.app_id = 1
      
      if (workflowConfig.value.id) {
        // 更新现有工作流
        const response = await WorkflowAPI.updateWorkflow(workflowConfig.value.id, request)
        if (response.code === 1) {
          ElMessage.success(response.message || '节点配置已保存')
        } else {
          ElMessage.error(response.message || '保存失败')
        }
      } else {
        // 创建新工作流
        const response = await WorkflowAPI.createWorkflow(request)
        if (response.code === 1 && response.data) {
          workflowConfig.value.id = String(response.data)
          ElMessage.success(response.message || '节点配置已保存')
        } else {
          ElMessage.error(response.message || '保存失败')
        }
      }
    } catch (error) {
      console.error('保存节点配置失败:', error)
      ElMessage.error('保存失败，请稍后重试')
    }
  }
}

const handleNodeDelete = (nodeId: string) => {
  ElMessageBox.confirm('确定要删除此节点吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 删除节点
    const nodeIndex = workflowConfig.value.nodes.findIndex(n => n.id === nodeId)
    if (nodeIndex > -1) {
      workflowConfig.value.nodes.splice(nodeIndex, 1)
    }
    
    // 删除相关连线
    workflowConfig.value.edges = workflowConfig.value.edges.filter(
      edge => edge.source !== nodeId && edge.target !== nodeId
    )
    
    selectedNode.value = null
    showSidebar.value = false
    ElMessage.success('节点已删除')
  }).catch(() => {
    // 用户取消删除
  })
}

const handleWorkflowSave = (request: WorkflowConfigAddRequest) => {
  console.log('Workflow save request:', request)
}

const handleWorkflowValidate = (result: { isValid: boolean; errors: string[] }) => {
  if (result.isValid) {
    ElMessage.success('工作流验证通过')
  } else {
    ElMessage.error(`工作流验证失败：${result.errors.join(', ')}`)
  }
}

// 工具栏操作
const handleSave = async () => {
  try {
    console.log('=== 开始保存工作流 ===')
    console.log('当前工作流配置:', {
      name: workflowConfig.value.name,
      description: workflowConfig.value.description,
      nodes: workflowConfig.value.nodes.length + ' 个节点',
      edges: workflowConfig.value.edges.length + ' 条边',
      id: workflowConfig.value.id
    })
    
    const validation = WorkflowTransform.validateWorkflowConfig(workflowConfig.value)
    if (!validation.isValid) {
      console.error('工作流验证失败:', validation.errors)
      ElMessage.error(`保存失败：${validation.errors.join(', ')}`)
      return
    }
    
    // 构建请求数据，确保包含 app_id
    const request = WorkflowTransform.toBackendRequest(workflowConfig.value)
    request.app_id = Date.now()
    
    // 调用API保存工作流
    if (workflowConfig.value.id) {
      // 更新现有工作流
      console.log('更新现有工作流，ID:', workflowConfig.value.id)
      const response = await WorkflowAPI.updateWorkflow(workflowConfig.value.id, request)
      
      if (response.code === 1) {
        ElMessage.success(response.message || '工作流已更新')
      } else {
        ElMessage.error(response.message || '更新失败')
      }
    } else {
      // 创建新工作流
      console.log('创建新工作流')
      const response = await WorkflowAPI.createWorkflow(request)
      
      if (response.code === 1 && response.data) {
        workflowConfig.value.id = response.data.toString()
        console.log('工作流创建成功，新ID:', workflowConfig.value.id)
        ElMessage.success(response.message || '工作流已创建')
      } else {
        ElMessage.error(response.message || '创建失败')
      }
    }
  } catch (error) {
    console.error('保存工作流异常:', error)
    ElMessage.error('保存失败')
  }
}

const handleRun = async () => {
  try {
    console.log('=== 准备运行工作流 ===')
    
    const validation = WorkflowTransform.validateWorkflowConfig(workflowConfig.value)
    if (!validation.isValid) {
      console.error('工作流验证失败:', validation.errors)
      ElMessage.error(`运行失败：${validation.errors.join(', ')}`)
      return
    }
    
    // 如果没有保存过，先保存工作流
    if (!workflowConfig.value.id) {
      console.log('工作流未保存，先保存工作流')
      await handleSave()
      if (!workflowConfig.value.id) {
        ElMessage.error('工作流保存失败，无法运行')
        return
      }
    }
    
    // 初始化输入参数对象和模式
    inputParams.value = {}
    inputParamModes.value = {}
    startNodeParams.value.forEach((param: any) => {
      inputParams.value[param.key] = param.default_value || ''
      // Image 类型默认使用上传模式
      if (param.type === 'Image') {
        inputParamModes.value[param.key] = 'upload'
      }
    })
    
    // 显示输入参数对话框
    showInputParamsDialog.value = true
    
  } catch (error) {
    console.error('准备运行失败:', error)
    ElMessage.error('准备运行失败')
  }
}

const handleRunWithParams = async () => {
  try {
    console.log('=== 开始运行工作流 ===')
    console.log('输入参数:', inputParams.value)
    
    // 验证必填参数
    for (const param of startNodeParams.value) {
      if (param.required && !inputParams.value[param.key]) {
        ElMessage.warning(`参数 "${param.key}" 是必填的`)
        return
      }
      
      // 验证 Image 类型参数
      if (param.type === 'Image' && param.required && !inputParams.value[param.key]) {
        ElMessage.warning(`请上传或输入 "${param.key}" 的图片`)
        return
      }
    }
    
    // 关闭输入参数对话框
    showInputParamsDialog.value = false
    
    // 显示运行状态对话框
    runStatus.value = 'running'
    runResults.value = null
    runError.value = ''
    showNodeDetails.value = false  // 重置节点详情折叠状态
    showRunDialog.value = true
    
    // 调用API运行工作流，传递输入参数
    const response = await WorkflowAPI.executeWorkflow(workflowConfig.value.id!, inputParams.value)
    console.log('运行工作流响应:', response)
    
    if (response.code === 1 && response.data) {
      const workflowInstanceId = response.data.toString()
      console.log('工作流实例ID:', workflowInstanceId)
      
      // 轮询获取运行结果
      const pollResult = async () => {
        try {
          console.log('轮询获取运行结果，实例ID:', workflowInstanceId)
          const resultResponse = await WorkflowAPI.getWorkflowResult(workflowInstanceId)
          console.log('运行结果响应:', resultResponse)
          
          if (resultResponse.code === 1 && resultResponse.data) {
            const resultData: any = resultResponse.data
            const workflowInstance = resultData.workflowInstanceVO
            const nodeInstances = resultData.workflowNodeInstanceVOList || []
            
            console.log('工作流实例状态:', workflowInstance.status)
            console.log('节点实例列表:', nodeInstances)
            
            // 根据工作流实例状态判断是否完成
            switch (workflowInstance.status) {
              case 'SUCCESS':
                console.log('工作流执行成功')
                runStatus.value = 'completed'
                runResults.value = resultData
                break
              case 'FAIL':
                console.log('工作流执行失败')
                runStatus.value = 'error'
                runError.value = '工作流执行失败'
                break
              case 'STOP':
                console.log('工作流被停止')
                runStatus.value = 'error'
                runError.value = '工作流被停止'
                break
              case 'TIMEOUT':
                console.log('工作流执行超时')
                runStatus.value = 'error'
                runError.value = '工作流执行超时'
                break
              case 'EXECUTING':
                console.log('工作流仍在执行中，2秒后继续轮询')
                // 继续轮询
                setTimeout(pollResult, 2000)
                break
              default:
                console.log('未知状态:', workflowInstance.status, '2秒后继续轮询')
                // 继续轮询
                setTimeout(pollResult, 2000)
                break
            }
          } else {
            console.log('获取运行结果失败，2秒后继续轮询')
            // 继续轮询
            setTimeout(pollResult, 2000)
          }
        } catch (error) {
          console.error('轮询运行结果异常:', error)
          runStatus.value = 'error'
          runError.value = String(error)
        }
      }
      
      // 延迟2秒后开始轮询
      setTimeout(pollResult, 2000)
    } else {
      console.error('工作流启动失败:', response)
      runStatus.value = 'error'
      runError.value = `工作流启动失败：${response.message || '未知错误'}`
    }
    
  } catch (error) {
    runStatus.value = 'error'
    runError.value = String(error)
    console.error('Run error:', error)
  }
}

const handleClear = () => {
  ElMessageBox.confirm('确定要清空整个工作流吗？', '确认清空', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    console.log('=== WorkflowDesigner: 清空工作流 ===')
    console.log('清空前节点数量:', workflowConfig.value.nodes.length)
    
    // 调用 WorkflowEditor 暴露的清空方法（使用 Vue Flow API）
    if (workflowEditorRef.value) {
      workflowEditorRef.value.clearWorkflow()
    }
    
    // 清空本地数据
    workflowConfig.value.nodes = []
    workflowConfig.value.edges = []
    selectedNode.value = null
    showSidebar.value = false
    
    console.log('清空后节点数量:', workflowConfig.value.nodes.length)
    
    // 重置节点计数器
    WorkflowTransform.resetNodeCounters()
    
    ElMessage.success('工作流已清空')
  }).catch(() => {
    // 用户取消清空
  })
}

const handleStopRun = () => {
  runStatus.value = 'idle'
  showRunDialog.value = false
  ElMessage.info('运行已停止')
}

// 处理参数图片上传
const handleParamImageUpload = async (file: any, paramKey: string) => {
  try {
    isUploadingParam.value = true
    console.log('开始上传参数图片:', file.name, 'for key:', paramKey)
    
    // 调用后端接口上传图片
    const response = await WorkflowAPI.uploadImage(file.raw)
    
    if (response.code === 1 && response.data) {
      // 将返回的URL保存到参数中
      inputParams.value[paramKey] = response.data.fileUrl
      
      console.log('参数图片上传成功，URL:', response.data.fileUrl)
      ElMessage.success('图片上传成功')
    } else {
      console.error('图片上传失败:', response.message)
      ElMessage.error(`图片上传失败: ${response.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('图片上传异常:', error)
    ElMessage.error('图片上传失败')
  } finally {
    isUploadingParam.value = false
  }
}

// 返回工作台页面
const handleBack = () => {
  router.push({ path: '/app', query: { tab: 'workbench' } })
}

// 根据ID加载工作流
const loadWorkflowById = async (id: string) => {
  isLoading.value = true
  
  try {
    console.log('=== 加载工作流 ===')
    console.log('工作流ID:', id)
    
    const response = await WorkflowAPI.getWorkflow(id)
    
    if (response.code === 1 && response.data) {
      const workflowData = response.data
      
      // 解析 JSON 字符串
      const nodes = JSON.parse(workflowData.nodes)
      const edges = JSON.parse(workflowData.edges)
      
      // 构建 WorkflowConfigAddRequest 格式
      const backendConfig: WorkflowConfigAddRequest = {
        name: workflowData.name,
        description: workflowData.description,
        app_id: parseInt(workflowData.appId),
        version: workflowData.version,
        nodes: nodes,
        edges: edges,
        canvas: workflowData.canvas
      }
      
      // 转换为前端格式并加载
      const frontendConfig = WorkflowTransform.toFrontendConfig(backendConfig, id)
      
      // 同步节点计数器
      WorkflowTransform.syncNodeCounters(frontendConfig.nodes)
      
      // 直接赋值，触发响应式更新
      workflowConfig.value = {
        ...frontendConfig,
        id: id  // 确保ID被保存
      }
      
      ElMessage.success('工作流加载成功')
    } else {
      ElMessage.error(response.message || '加载工作流失败')
    }
  } catch (error) {
    console.error('加载工作流失败:', error)
    ElMessage.error('加载工作流失败')
  } finally {
    isLoading.value = false
  }
}

const handleSettingsSave = async () => {
  // 更新工作流配置
  workflowConfig.value.name = settingsForm.value.name
  workflowConfig.value.description = settingsForm.value.description
  workflowConfig.value.version = settingsForm.value.version
  
  if (workflowConfig.value.settings) {
    workflowConfig.value.settings.snapToGrid = settingsForm.value.snapToGrid
    workflowConfig.value.settings.showGrid = settingsForm.value.showGrid
    workflowConfig.value.settings.showMinimap = settingsForm.value.showMinimap
  }
  
  showSettings.value = false
  
  // 调用接口保存工作流
  try {
    const request = WorkflowTransform.toBackendRequest(workflowConfig.value)
    request.app_id = 1
    
    if (workflowConfig.value.id) {
      // 更新现有工作流
      const response = await WorkflowAPI.updateWorkflow(workflowConfig.value.id, request)
      if (response.code === 1) {
        ElMessage.success(response.message || '工作流设置已保存')
      } else {
        ElMessage.error(response.message || '保存失败')
      }
    } else {
      // 创建新工作流
      const response = await WorkflowAPI.createWorkflow(request)
      if (response.code === 1 && response.data) {
        workflowConfig.value.id = String(response.data)
        ElMessage.success(response.message || '工作流已创建并保存')
      } else {
        ElMessage.error(response.message || '保存失败')
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  }
}

// 显示运行记录
const handleShowRunHistory = async () => {
  if (!workflowConfig.value.id) {
    ElMessage.warning('请先保存工作流')
    return
  }
  
  showRunHistoryDialog.value = true
  await loadRunHistory()
}

// 加载运行记录
const loadRunHistory = async () => {
  try {
    isLoadingHistory.value = true
    
    const response = await WorkflowAPI.getWorkflowInstances({
      workflowConfigId: workflowConfig.value.id!,  // 直接传字符串，避免精度丢失
      pageNum: historyPageNum.value,
      pageSize: historyPageSize.value
    })
    
    if (response.code === 1 && response.data) {
      runHistoryList.value = response.data.list
      historyTotal.value = parseInt(response.data.total)
    } else {
      ElMessage.error(response.message || '加载运行记录失败')
    }
  } catch (error) {
    console.error('加载运行记录失败:', error)
    ElMessage.error('加载运行记录失败')
  } finally {
    isLoadingHistory.value = false
  }
}

// 分页变化
const handleHistoryPageChange = (page: number) => {
  historyPageNum.value = page
  loadRunHistory()
}

const handleHistoryPageSizeChange = (size: number) => {
  historyPageSize.value = size
  historyPageNum.value = 1
  loadRunHistory()
}

// 查看运行结果
const handleViewRunResult = async (instanceId: number) => {
  try {
    console.log('查看运行结果，实例ID:', instanceId)
    
    const resultResponse = await WorkflowAPI.getWorkflowResult(instanceId.toString())
    console.log('运行结果响应:', resultResponse)
    
    if (resultResponse.code === 1 && resultResponse.data) {
      // 关闭运行记录对话框
      showRunHistoryDialog.value = false
      
      // 设置运行结果并显示结果对话框
      runResults.value = resultResponse.data
      runStatus.value = 'completed'
      showNodeDetails.value = false
      showRunDialog.value = true
    } else {
      ElMessage.error(resultResponse.message || '获取运行结果失败')
    }
  } catch (error) {
    console.error('获取运行结果失败:', error)
    ElMessage.error('获取运行结果失败')
  }
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  const statusMap: Record<string, any> = {
    'SUCCESS': 'success',
    'EXECUTING': 'warning',
    'FAIL': 'danger',
    'STOP': 'info',
    'TIMEOUT': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'SUCCESS': '成功',
    'EXECUTING': '执行中',
    'FAIL': '失败',
    'STOP': '已停止',
    'TIMEOUT': '超时'
  }
  return statusMap[status] || status
}
</script>

<style scoped>
/* OpenAI 风格 - 简洁优雅的顶部导航栏 */
.workflow-designer {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
  overflow: hidden;
  position: relative;
}

.designer-header {
  height: 56px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(8px);
}

.header-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-button {
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  border: 1px solid #e5e7eb;
  background: white;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.back-button:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #111827;
}

.workflow-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: -0.01em;
}

.header-center {
  flex: 0 0 auto;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 按钮组样式优化 */
:deep(.el-button-group) {
  display: flex;
  gap: 6px;
  border: none;
}

:deep(.el-button-group .el-button) {
  margin: 0;
  border-radius: 8px !important;
  font-size: 13px;
  font-weight: 500;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #e5e7eb;
  background: white;
  color: #374151;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-button-group .el-button:hover) {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #111827;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

:deep(.el-button-group .el-button:active) {
  transform: translateY(0);
}

:deep(.el-button.is-circle) {
  width: 36px;
  height: 36px;
  border-radius: 8px !important;
  border: 1px solid #e5e7eb;
  background: white;
  color: #6b7280;
  transition: all 0.15s ease;
}

:deep(.el-button.is-circle:hover) {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #111827;
}

.designer-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.node-palette-wrapper {
  flex-shrink: 0;
}

:deep(.el-dialog.settings-dialog) {
  max-width: 560px;
}

:deep(.el-dialog.settings-dialog .el-dialog__body) {
  overflow-x: auto;
}

:deep(.el-dialog.run-history-dialog) {
  max-width: 900px;
}

:deep(.el-dialog.run-result-dialog) {
  max-width: 720px;
}

:deep(.el-dialog.run-result-dialog .el-dialog__body) {
  padding: 0;
}

:deep(.el-dialog.input-params-dialog) {
  max-width: 600px;
}

:deep(.el-dialog.input-params-dialog .el-dialog__body) {
  padding: 0;
}

.run-history-table-scroll {
  overflow-x: auto;
}

.scroll-x {
  overflow-x: auto;
  max-width: 100%;
  -webkit-overflow-scrolling: touch;
}

/* 展开节点库按钮（节点库收起时显示） */
.mobile-add-node-btn {
  display: block;
  position: absolute;
  left: 12px;
  top: 68px;
  z-index: 100;
}

.mobile-add-node-btn .el-button {
  width: 44px;
  height: 44px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 桌面端显示按钮组，移动端隐藏 */
.desktop-buttons {
  display: flex;
}

/* 移动端下拉菜单（默认隐藏） */
.mobile-dropdown {
  display: none;
}

/* 手机端样式 */
@media (max-width: 768px) {
  .designer-header {
    padding: 0 12px;
  }
  
  .workflow-title {
    font-size: 14px;
  }
  
  /* 隐藏桌面端按钮组 */
  .desktop-buttons {
    display: none !important;
  }
  
  /* 显示移动端下拉菜单 */
  .mobile-dropdown {
    display: block;
  }
  
  /* 移动端仍保持 Header 下方左侧 */
  .mobile-add-node-btn {
    left: 12px;
    top: 68px;
  }
  
  /* 节点面板样式 */
  .node-palette-wrapper {
    position: absolute;
    left: 0;
    top: 56px;
    bottom: 0;
    width: 280px;
    background: white;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
    z-index: 99;
  }
}

.canvas-container {
  flex: 1;
  position: relative;
  background: white;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
}

/* 运行对话框样式 */
.run-result-content {
  padding: 0;
}

.run-result-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 16px;
}

.run-dialog-content {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
  padding: 16px;
  text-align: center;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-sizing: border-box;
}

.input-params-content {
  padding: 0;
}

.input-params-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 16px;
}

.input-params-box {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-sizing: border-box;
}

.running-status,
.completed-status,
.error-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.success-icon {
  color: #67c23a;
  font-size: 48px;
}

.error-icon {
  color: #f56c6c;
  font-size: 48px;
}

.run-results,
.error-details {
  margin-top: 20px;
  text-align: left;
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.run-results h4,
.error-details h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.run-results pre,
.error-details pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
}

/* 工作流执行信息样式 */
.workflow-info {
  background: #f0f9ff;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #bae6fd;
  margin-bottom: 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.node-output,
.node-error {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.workflow-info p {
  margin: 4px 0;
  font-size: 14px;
}

/* 节点执行结果样式 */
.node-results {
  margin-top: 12px;
  margin-bottom: 16px;
}

.node-result-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.node-result-item.node-success {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.node-result-item.node-executing {
  background: #fef3c7;
  border-color: #fde68a;
}

.node-result-item.node-failed {
  background: #fef2f2;
  border-color: #fecaca;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.node-name {
  font-weight: 600;
  color: #1f2937;
}

.node-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.node-result-item.node-success .node-status {
  background: #dcfce7;
  color: #166534;
}

.node-result-item.node-executing .node-status {
  background: #fef3c7;
  color: #92400e;
}

.node-result-item.node-failed .node-status {
  background: #fecaca;
  color: #991b1b;
}

.node-time {
  font-size: 12px;
  color: #6b7280;
}

.node-output,
.node-error {
  font-size: 13px;
  margin-top: 8px;
  padding: 8px;
  border-radius: 4px;
}

.node-output {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  color: #0c4a6e;
}

.node-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

/* 结束节点输出结果样式 */
.end-node-output {
  margin-top: 20px;
  padding: 0;
  background: transparent;
}

.end-node-output h4 {
  margin: 0 0 12px 0;
  color: #374151;
  font-size: 15px;
  font-weight: 600;
}

.end-node-output .output-content {
  background: linear-gradient(to bottom right, #f0f9ff, #e0f2fe);
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #bae6fd;
  color: #0c4a6e;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.end-node-output .output-content:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
  border-color: #7dd3fc;
}

/* 节点详情折叠区域样式 */
.node-details-section {
  margin-top: 20px;
}

.details-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.details-header:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.details-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.toggle-icon {
  transition: transform 0.3s ease;
  color: #6b7280;
}

.toggle-icon.expanded {
  transform: rotate(90deg);
}

/* 输入参数描述样式 */
.param-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}

/* Image 参数行（左右布局） */
.image-param-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}

/* 左侧：模式按钮（上下排列） */
.mode-buttons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  align-items: flex-start;
}

.mode-buttons .el-button {
  width: 90px;
  justify-content: flex-start;
  padding-left: 12px;
}

/* 右侧：内容区域 */
.mode-content {
  flex: 1;
  min-width: 0;
}

/* 已上传内容（图片+删除按钮） */
.uploaded-content {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 55px;
}

.uploaded-image {
  width: 80px;
  height: 100%;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.image-error-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

/* 运行记录对话框样式 */
.run-history-content {
  padding: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background: #f9fafb;
  color: #374151;
  font-weight: 600;
}

:deep(.el-table td) {
  color: #6b7280;
}

:deep(.el-table .el-button--small) {
  padding: 5px 12px;
  font-size: 13px;
}
</style>
