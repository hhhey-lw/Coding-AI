<template>
  <div ref="flowWrapperRef" class="workflow-editor" @contextmenu.prevent>
    <VueFlow
      :nodes="nodes"
      :edges="edges"
      :class="{ dark }"
      class="vue-flow"
      :default-viewport="{ zoom: 1 }"
      :min-zoom="0.2"
      :max-zoom="4"
      :default-edge-options="defaultEdgeOptions"
      :connection-line-style="connectionLineStyle"
      :snap-to-grid="workflowConfig.settings?.snapToGrid"
      :snap-grid="[workflowConfig.settings?.gridSize || 15, workflowConfig.settings?.gridSize || 15]"
      @node-click="handleNodeClick"
      @edge-context-menu="handleEdgeContextMenu"
      @pane-click="handlePaneClick"
      @drop="handleDrop"
      @dragover="handleDragOver"
    >
      <!-- 自定义节点 -->
      <template #node-custom="{ data, id }">
        <CustomNode
          :node-id="id"
          :node-type="data.type"
          :node-data="data"
          :selected="selectedNodeId === id"
          @node-click="handleCustomNodeClick"
          @node-delete="handleNodeDelete"
        />
      </template>

      <!-- ✅ 使用 Vue Flow 的标准背景组件 -->
      <Background 
        v-if="workflowConfig.settings?.showGrid"
        :size="2" 
        :gap="20" 
        pattern-color="#BDBDBD" 
      />
      
      <!-- ✅ 使用 Vue Flow 的标准控制组件 -->
      <Controls 
        :show-zoom="true"
        :show-fit-view="true"
        :show-interactive="true"
      />
      
      <!-- MiniMap 已隐藏 -->
    </VueFlow>
    
    <!-- 边右键菜单 -->
    <div 
      v-if="edgeContextMenu.visible"
      class="edge-context-menu"
      :style="{
        left: edgeContextMenu.x + 'px',
        top: edgeContextMenu.y + 'px'
      }"
    >
      <el-button 
        type="danger" 
        size="small"
        @click="handleDeleteEdge"
      >
        <el-icon><Delete /></el-icon>
        删除连线
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { VueFlow, useVueFlow, MarkerType } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { ElMessage } from 'element-plus'
import type { 
  WorkflowConfig, 
  WorkflowNode, 
  WorkflowEdge,
  WorkflowConfigAddRequest,
} from '@/types/workflow'
import { WorkflowTransform } from '@/utils/workflowTransform'
import NodeCreator from '@/utils/nodeCreator'
import CustomNode from './CustomNode.vue'

// Vue Flow样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

// ✅ Props 定义（必须在最前面）
interface Props {
  modelValue?: WorkflowConfig
  readonly?: boolean
}

const getVueFlowBounds = (): DOMRect | null => {
  if (!flowWrapperRef.value) return null
  const el = flowWrapperRef.value.querySelector('.vue-flow') as HTMLElement | null
  return (el || flowWrapperRef.value).getBoundingClientRect()
}

const addNodeFromPaletteAtClientPoint = (
  type: string,
  clientPoint: { x: number; y: number },
  nodeData?: any
) => {
  const bounds = getVueFlowBounds()
  if (!bounds) return null

  const position = project({
    x: clientPoint.x - bounds.left,
    y: clientPoint.y - bounds.top
  })

  return createNodeFromPalette(type, position, nodeData)
}

const addNodeFromPaletteAtCenter = (type: string, nodeData?: any) => {
  const bounds = getVueFlowBounds()
  if (!bounds) return null

  const position = project({
    x: bounds.width / 2,
    y: bounds.height / 2
  })

  return createNodeFromPalette(type, position, nodeData)
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false
})

// ✅ Emits 定义（必须在computed使用前）
const emit = defineEmits<{
  'update:modelValue': [value: WorkflowConfig]
  'node-click': [node: WorkflowNode]
  'edge-click': [edge: WorkflowEdge]
  'canvas-click': [event: MouseEvent]
  'save': [request: WorkflowConfigAddRequest]
  'validate': [result: { isValid: boolean; errors: string[] }]
}>()

// Vue Flow实例
const { onConnect, addEdges, project, fitView, onNodeDragStop, onNodesChange, onEdgesChange } = useVueFlow()

const flowWrapperRef = ref<HTMLElement | null>(null)

// 响应式数据
const dark = ref(false)
const selectedNodeId = ref<string | null>(null)
const edgeContextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  edgeId: ''
})
const currentWorkflowId = ref<string | undefined>(undefined)

// ✅ 内部工作流配置（不直接绑定到 VueFlow）
const internalNodes = ref<WorkflowNode[]>([])
const internalEdges = ref<WorkflowEdge[]>([])

const workflowConfig = ref<WorkflowConfig>({
  name: 'New Workflow',
  description: '',
  version: '1.0.0',
  nodes: [],
  edges: [],
  viewport: { x: 0, y: 0, zoom: 1 },
  settings: {
    snapToGrid: false,
    gridSize: 15,
    showGrid: true,
    showMinimap: true
  }
})

// ✅ Vue Flow 边的默认选项
const defaultEdgeOptions = {
  type: 'default',  // 使用贝塞尔曲线
  animated: false,
  deletable: true,  // 允许删除边（选中后按 Delete 键）
  markerEnd: MarkerType.ArrowClosed,  // 箭头标记
  style: { 
    strokeWidth: 2, 
    stroke: '#B1B1B7'  // 黑灰色
  }
}

// ✅ 连接线样式（拖动连接时的临时线）
const connectionLineStyle = {
  strokeWidth: 2,
  stroke: '#67c23a'  // 拖动时保持绿色以便区分
}

// ✅ 使用 computed getter 转换为 VueFlow 格式（单向绑定）
const nodes = computed(() => {
  return internalNodes.value.map(node => ({
    id: node.id,
    type: 'custom',
    position: node.position,
    data: node.data
  }))
})

const edges = computed(() => {
  return internalEdges.value.map(edge => {
    // ✅ 直接使用存储的 handle ID，确保精准连接
    const sourceHandle = edge.sourceHandle || `${edge.source}-output`
    const targetHandle = edge.targetHandle || `${edge.target}-input`
    
    return {
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: sourceHandle,
      targetHandle: targetHandle,
      type: 'default',  // 贝塞尔曲线
      markerEnd: MarkerType.ArrowClosed,
      style: { strokeWidth: 2, stroke: '#B1B1B7' }  // 黑灰色
    }
  })
})

// ✅ 同步内部数据到 workflowConfig 并通知父组件
const syncToParent = () => {
  workflowConfig.value.nodes = [...internalNodes.value]
  workflowConfig.value.edges = [...internalEdges.value]
  emit('update:modelValue', workflowConfig.value)
}

// ✅ 优化：移除深层监听，改为事件驱动同步，解决拖拽卡顿问题
// 监听节点拖拽结束，同步位置
onNodeDragStop(({ nodes: draggedNodes }) => {
  draggedNodes.forEach((draggedNode: any) => {
    const internalNode = internalNodes.value.find(n => n.id === draggedNode.id)
    if (internalNode) {
      internalNode.position = { ...draggedNode.position }
    }
  })
  syncToParent()
})

// 监听节点变化（包括删除）
onNodesChange((changes) => {
  // 处理删除事件
  const deleteChanges = changes.filter(c => c.type === 'remove')
  if (deleteChanges.length > 0) {
    const idsToRemove = new Set(deleteChanges.map(c => c.id))
    internalNodes.value = internalNodes.value.filter(n => !idsToRemove.has(n.id))
    syncToParent()
  }
})

// 监听边变化（包括删除）
onEdgesChange((changes) => {
  // 处理删除事件
  const deleteChanges = changes.filter(c => c.type === 'remove')
  if (deleteChanges.length > 0) {
    const idsToRemove = new Set(deleteChanges.map(c => c.id))
    internalEdges.value = internalEdges.value.filter(e => !idsToRemove.has(e.id))
    syncToParent()
  }
})

// ✅ 监听 props 变化，只在工作流 ID 变化时完整加载
watch(() => props.modelValue, async (newValue) => {
  if (!newValue) return
  
  const isNewWorkflow = newValue.id !== currentWorkflowId.value
  
  if (isNewWorkflow) {
    console.log('=== WorkflowEditor: 加载新工作流 ===')
    console.log('工作流ID:', newValue.id)
    
    currentWorkflowId.value = newValue.id
    workflowConfig.value = { ...newValue }
    
    // 更新内部数据
    internalNodes.value = newValue.nodes ? [...newValue.nodes] : []
    internalEdges.value = newValue.edges ? [...newValue.edges] : []
    
    await nextTick()
    
    if (newValue.nodes && newValue.nodes.length > 0) {
      setTimeout(() => {
        fitView({ padding: 0.2, duration: 300 })
      }, 100)
    }
  } else {
    // 增量更新：只更新基本信息，保留节点位置
    workflowConfig.value.name = newValue.name
    workflowConfig.value.description = newValue.description
    workflowConfig.value.version = newValue.version
    workflowConfig.value.settings = newValue.settings
    
    // 只更新节点数据，不改变位置
    if (newValue.nodes) {
      newValue.nodes.forEach(newNode => {
        const existingNode = internalNodes.value.find(n => n.id === newNode.id)
        if (existingNode) {
          // 保留位置，只更新其他数据
          Object.assign(existingNode, {
            ...newNode,
            position: existingNode.position
          })
        } else {
          internalNodes.value.push(newNode)
        }
      })
      
      const newNodeIds = new Set(newValue.nodes.map(n => n.id))
      internalNodes.value = internalNodes.value.filter(n => newNodeIds.has(n.id))
    }
    
    if (newValue.edges) {
      internalEdges.value = [...newValue.edges]
    }
  }
})

// ✅ 连接处理 - 使用 Vue Flow 的 addEdges API
onConnect((params: any) => {
  // 验证连接规则
  if (!validateConnection(params)) {
    console.warn('连接被拒绝:', params)
    return
  }
  
  // ✅ 使用 Vue Flow 传递的 handle ID（因为它知道用户点击了哪个 Handle）
  // 只在缺失时才使用默认值
  const sourceHandle = params.sourceHandle || `${params.source}-output`
  const targetHandle = params.targetHandle || `${params.target}-input`
  
  // 验证格式是否符合规范（只做检查，不强制修改）
  const isValidSourceHandle = sourceHandle.includes(`${params.source}-output`)
  const isValidTargetHandle = targetHandle === `${params.target}-input`
  
  if (!isValidSourceHandle) {
    console.warn('⚠️ sourceHandle 格式异常:', {
      sourceHandle: sourceHandle,
      期望包含: `${params.source}-output`
    })
  }
  
  if (!isValidTargetHandle) {
    console.warn('⚠️ targetHandle 格式异常:', {
      targetHandle: targetHandle,
      期望值: `${params.target}-input`
    })
  }
  
  console.log('✅ 创建边:', {
    '源节点ID': params.source,
    '目标节点ID': params.target,
    'sourceHandle': sourceHandle,
    'targetHandle': targetHandle,
    'Vue Flow原始sourceHandle': params.sourceHandle,
    'Vue Flow原始targetHandle': params.targetHandle,
    '是否修正了sourceHandle': params.sourceHandle !== sourceHandle,
    '是否修正了targetHandle': params.targetHandle !== targetHandle
  })
  
  // ✅ 使用 Vue Flow 的 addEdges API
  addEdges([{
    id: WorkflowTransform.generateEdgeId(params.source, params.target),
    source: params.source,
    target: params.target,
    sourceHandle: sourceHandle,
    targetHandle: targetHandle,
    ...defaultEdgeOptions  // 应用默认边选项
  }])
  
  // ✅ 手动同步到 internalEdges (替代原来的 watcher)
  // 注意：VueFlow addEdges 会自动更新 edges.value，所以这里只需要同步到 internalEdges
  // 但为了保险起见，我们直接调用 syncToParent
  setTimeout(() => {
    const newEdge: WorkflowEdge = {
        id: WorkflowTransform.generateEdgeId(params.source, params.target),
        source: params.source,
        target: params.target,
        sourceHandle: sourceHandle,
        targetHandle: targetHandle
    }
    const exists = internalEdges.value.some(e => e.id === newEdge.id)
    if (!exists) {
        internalEdges.value.push(newEdge)
        syncToParent()
    }
  }, 10)
})

// 生命周期
onMounted(() => {
  console.log('WorkflowEditor mounted, Vue Flow integrated')
})

// 连接验证
const validateConnection = (connection: any): boolean => {
  const sourceNode = internalNodes.value.find(n => n.id === connection.source)
  const targetNode = internalNodes.value.find(n => n.id === connection.target)
  
  if (!sourceNode || !targetNode) {
    console.error('节点不存在')
    ElMessage.error('连接失败：节点不存在')
    return false
  }
  
  // 规则1: 不能连接自己
  if (connection.source === connection.target) {
    console.warn('不能连接自己')
    ElMessage.warning('不能连接节点自身')
    return false
  }
  
  // 规则2: 开始节点不能作为目标（没有入连接点）
  if (targetNode.type === 'Start') {
    console.warn('开始节点不能作为连接目标')
    ElMessage.warning('开始节点不能作为连接目标')
    return false
  }
  
  // 规则3: 结束节点不能作为源（没有出连接点）
  if (sourceNode.type === 'End') {
    console.warn('结束节点不能作为连接源')
    ElMessage.warning('结束节点不能作为连接源')
    return false
  }
  
  // 规则4: 检查连接句柄类型匹配
  // sourceHandle应该是output类型，targetHandle应该是input类型
  if (connection.sourceHandle && !connection.sourceHandle.includes('output')) {
    console.warn('源句柄必须是输出类型')
    ElMessage.warning('只能从输出连接点发起连接')
    return false
  }
  
  if (connection.targetHandle && !connection.targetHandle.includes('input')) {
    console.warn('目标句柄必须是输入类型')
    ElMessage.warning('只能连接到输入连接点')
    return false
  }
  
  // 规则5: 防止重复连接
  const existingEdge = internalEdges.value.find(edge => 
    edge.source === connection.source && edge.target === connection.target
  )
  if (existingEdge) {
    console.warn('节点间已存在连接')
    ElMessage.warning('这两个节点间已存在连接')
    return false
  }
  
  // 规则6: 防止循环连接（简单检测）
  if (wouldCreateCycle(connection.source, connection.target)) {
    console.warn('连接会创建循环，被拒绝')
    ElMessage.warning('此连接会形成循环，不允许创建')
    return false
  }
  
  console.log('连接验证通过')
  return true
}

// 检测是否会创建循环
const wouldCreateCycle = (sourceId: string, targetId: string): boolean => {
  // 使用深度优先搜索检测是否存在从target到source的路径
  const visited = new Set<string>()
  
  const dfs = (currentId: string): boolean => {
    if (visited.has(currentId)) return false
    if (currentId === sourceId) return true
    
    visited.add(currentId)
    
    // 查找当前节点的所有输出边
    const outgoingEdges = internalEdges.value.filter(edge => edge.source === currentId)
    
    for (const edge of outgoingEdges) {
      if (dfs(edge.target)) return true
    }
    
    return false
  }
  
  return dfs(targetId)
}

// 事件处理方法
const handleNodeClick = (event: any) => {
  selectedNodeId.value = event.node.id
  emit('node-click', event.node)
}

const handlePaneClick = (event: MouseEvent) => {
  selectedNodeId.value = null
  edgeContextMenu.value.visible = false  // 关闭边菜单
  emit('canvas-click', event)
}

const handleEdgeContextMenu = (event: any) => {
  event.event.preventDefault()
  
  // 显示右键菜单
  edgeContextMenu.value = {
    visible: true,
    x: event.event.clientX,
    y: event.event.clientY,
    edgeId: event.edge.id
  }
  
  console.log('✅ 边右键菜单:', edgeContextMenu.value)
}

const handleDeleteEdge = () => {
  if (edgeContextMenu.value.edgeId) {
    deleteEdge(edgeContextMenu.value.edgeId)
    ElMessage.success('连线已删除')
  }
  edgeContextMenu.value.visible = false
}

const handleCustomNodeClick = (nodeId: string) => {
  selectedNodeId.value = nodeId
  const node = internalNodes.value.find(n => n.id === nodeId)
  if (node) {
    emit('node-click', node)
  }
}

const handleNodeDelete = (nodeId: string) => {
  deleteNode(nodeId)
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  
  if (event.dataTransfer) {
    try {
      const nodeData = JSON.parse(event.dataTransfer.getData('application/json'))
      
      // 获取Vue Flow容器的边界
      const vueFlowBounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
      
      // 计算相对于Vue Flow容器的位置
      const position = project({ 
        x: event.clientX - vueFlowBounds.left,
        y: event.clientY - vueFlowBounds.top
      })
      
      console.log('Drop position:', {
        clientX: event.clientX,
        clientY: event.clientY,
        boundsLeft: vueFlowBounds.left,
        boundsTop: vueFlowBounds.top,
        calculatedPosition: position
      })
      
      createNodeFromPalette(nodeData.type, position, nodeData)
    } catch (error) {
      console.error('Failed to parse dropped data:', error)
    }
  }
}

// 节点操作方法
const createNodeFromPalette = (type: string, position: { x: number; y: number }, nodeData?: any) => {
  try {
    // 使用NodeCreator创建节点
    let node: any
    
    switch (type) {
      case 'Start':
        node = NodeCreator.createStartNode({ 
          name: nodeData?.name || '开始节点'
        })
        break
      case 'End':
        node = NodeCreator.createEndNode({ 
          name: nodeData?.name || '结束节点',
          output_type: 'text',
          text_template: '',
          json_params: []
        })
        break
      case 'TextGen':
        node = NodeCreator.createLLMNode({
          name: nodeData?.name || 'LLM处理',
          sys_prompt_content: '',
          prompt_content: '',
          model_config: {
            provider: 'openai',
            model_id: 'gpt-4',
            model_name: 'GPT-4',
            params: []
          }
        })
        break
      case 'ImgGen':
        node = NodeCreator.createImageGenNode({
          name: nodeData?.name || '图像生成',
          provider: 'volcengine',
          model: 'doubao-seedream-4-0',
          prompt: '',
          imageUrls: [],
          maxImages: 1,
          imgSize: '4096×2160'
        })
        break
      case 'VideoGen':
        node = NodeCreator.createVideoGenNode({
          name: nodeData?.name || '视频生成',
          provider: 'BaiLian',
          model_id: 'wan2.2-kf2v-flash',
          input_prompt: '',
          first_frame_image: '',
          tail_frame_image: '',
          resolution: '1080P',
          duration: 5
        })
        break
      case 'MusicGen':
        node = NodeCreator.createMusicGenNode({
          name: nodeData?.name || '音乐生成',
          provider: 'minimax',
          model: 'music-1.5',
          prompt: '',
          lyrics: ''
        })
        break
      case 'MCP':
        node = NodeCreator.createMCPNode({
          name: nodeData?.name || 'MCP调用',
          server_code: 'example_server',
          server_name: 'Example Server',
          tool_name: 'example_tool'
        })
        break
      case 'Script':
        node = NodeCreator.createScriptNode({
          name: nodeData?.name || '脚本执行',
          scriptType: 'javascript',
          scriptContent: 'function main(params) { return params; }'
        })
        break
      case 'Email':
        node = NodeCreator.createEmailNode({
          name: nodeData?.name || '发送邮件',
          to: 'example@email.com',
          from: 'sender@email.com',
          subject: '邮件主题',
          content: '邮件内容',
          html: false,
          authorization: 'your_auth_token'
        })
        break
      case 'Judge':
        node = NodeCreator.createJudgeNode({
          name: nodeData?.name || '条件判断'
        })
        break
      default:
        console.warn('Unknown node type:', type)
        return null
    }
    
    // 转换为WorkflowNode格式
    const workflowNode: WorkflowNode = {
      id: node.id,
      type: node.type,
      position,
      data: {
        name: node.name,
        desc: node.desc,
        config: node.config,
        type: node.type
      }
    }
    
    internalNodes.value.push(workflowNode)
    syncToParent()
    
    console.log(`✅ 节点已创建: ${workflowNode.data.name} (${workflowNode.type})`, {
      totalNodes: internalNodes.value.length,
      position: workflowNode.position
    })
    
    return workflowNode
  } catch (error) {
    console.error('Failed to create node:', error)
    console.error('Error details:', error)
    return null
  }
}

const addNode = (type: string, position: { x: number; y: number }, nodeData?: Partial<WorkflowNode['data']>) => {
  const nodeId = WorkflowTransform.generateNodeId(type)
  const newNode: WorkflowNode = {
    id: nodeId,
    type,
    position,
    data: {
      name: nodeData?.name || `${type} Node`,
      desc: nodeData?.desc || '',
      config: nodeData?.config,
      type: type,
      ...nodeData
    }
  }
  
  internalNodes.value.push(newNode)
  syncToParent()
  console.log('Added node:', newNode)
  return newNode
}

const deleteNode = (nodeId: string) => {
  const nodeIndex = internalNodes.value.findIndex(n => n.id === nodeId)
  if (nodeIndex > -1) {
    internalNodes.value.splice(nodeIndex, 1)
  }
  
  internalEdges.value = internalEdges.value.filter(
    edge => edge.source !== nodeId && edge.target !== nodeId
  )
  
  syncToParent()
  console.log('Deleted node:', nodeId, {
    remainingNodes: internalNodes.value.length
  })
}

// ✅ 边操作方法
const addEdge = (source: string, target: string, sourceHandle?: string, targetHandle?: string) => {
  const newEdge: WorkflowEdge = {
    id: WorkflowTransform.generateEdgeId(source, target),
    source,
    target,
    sourceHandle: sourceHandle || `${source}-output`,
    targetHandle: targetHandle || `${target}-input`
  }
  
  internalEdges.value.push(newEdge)
  syncToParent()
  console.log('✅ 添加边:', newEdge)
  return newEdge
}

const deleteEdge = (edgeId: string) => {
  internalEdges.value = internalEdges.value.filter(e => e.id !== edgeId)
  syncToParent()
  console.log('✅ 删除边:', edgeId)
}

const validateWorkflow = () => {
  const result = WorkflowTransform.validateWorkflowConfig(workflowConfig.value)
  emit('validate', result)
  console.log('Validation result:', result)
  return result
}

const saveWorkflow = () => {
  const validation = validateWorkflow()
  if (validation.isValid) {
    const backendRequest = WorkflowTransform.toBackendRequest(workflowConfig.value)
    emit('save', backendRequest)
    console.log('Save workflow request:', backendRequest)
    return backendRequest
  } else {
    console.warn('Workflow validation failed:', validation.errors)
    return null
  }
}

const loadWorkflow = (config: WorkflowConfig | WorkflowConfigAddRequest) => {
  if ('viewport' in config) {
    // 已经是前端格式
    workflowConfig.value = config as WorkflowConfig
  } else {
    // 后端格式，需要转换
    workflowConfig.value = WorkflowTransform.toFrontendConfig(config as WorkflowConfigAddRequest)
  }
  console.log('Loaded workflow:', workflowConfig.value)
}

const clearWorkflow = () => {
  console.log('=== WorkflowEditor: 清空工作流 ===')
  
  internalNodes.value = []
  internalEdges.value = []
  
  workflowConfig.value = {
    name: 'New Workflow',
    description: '',
    version: '1.0.0',
    nodes: [],
    edges: [],
    viewport: { x: 0, y: 0, zoom: 1 },
    settings: workflowConfig.value.settings
  }
  
  syncToParent()
  console.log('✅ 工作流已清空')
}

const getWorkflowData = () => {
  return {
    frontend: workflowConfig.value,
    backend: WorkflowTransform.toBackendRequest(workflowConfig.value),
    validation: WorkflowTransform.validateWorkflowConfig(workflowConfig.value)
  }
}

// 暴露方法给父组件
defineExpose({
  addNode,
  createNodeFromPalette,
  deleteNode,
  addEdge,
  deleteEdge,
  saveWorkflow,
  loadWorkflow,
  clearWorkflow,
  validateWorkflow,
  getWorkflowData,
  addNodeFromPaletteAtClientPoint,
  addNodeFromPaletteAtCenter
})
</script>

<style scoped>
.workflow-editor {
  width: 100%;
  height: 100%;
  position: relative;
}

.vue-flow {
  background: #f8fafc;
}

/* Vue Flow主题自定义 */
:deep(.vue-flow__minimap) {
  transform: scale(0.75);
  transform-origin: bottom right;
}

:deep(.vue-flow__controls) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

:deep(.vue-flow__controls-button) {
  background: white;
  border-bottom: 1px solid #e4e7ed;
  transition: all 0.2s ease;
}

:deep(.vue-flow__controls-button:hover) {
  background: #f5f7fa;
}

:deep(.vue-flow__controls-button:last-child) {
  border-bottom: none;
}

/* ✅ 使用 Vue Flow 的默认样式，只做少量自定义 */

/* 背景网格样式 */
:deep(.vue-flow__background) {
  background-color: #f8fafc;
}

/* 深色主题支持 */
.vue-flow.dark {
  background: #1a1a1a;
}

.dark :deep(.vue-flow__background) {
  background-color: #1a1a1a;
}

/* 边右键菜单样式 */
.edge-context-menu {
  position: fixed;
  z-index: 9999;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.edge-context-menu :deep(.el-button) {
  width: 120px;
  justify-content: flex-start;
}
</style>
