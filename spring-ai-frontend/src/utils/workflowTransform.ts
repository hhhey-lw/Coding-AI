import type { 
  WorkflowNode, 
  WorkflowEdge, 
  WorkflowConfig,
  WorkflowConfigAddRequest,
  Node,
  Edge,
  NodeCustomConfig,
  InputParam,
  OutputParam
} from '@/types/workflow'

/**
 * 前端工作流数据转换工具类
 */
export class WorkflowTransform {
  
  /**
   * 将前端WorkflowConfig转换为后端WorkflowConfigAddRequest
   */
  static toBackendRequest(frontendConfig: WorkflowConfig): WorkflowConfigAddRequest {
    // 将前端画布数据序列化为JSON字符串
    const canvasData = {
      viewport: frontendConfig.viewport,
      settings: frontendConfig.settings,
      // 可以添加更多前端特有的数据
      nodePositions: frontendConfig.nodes.reduce((acc, node: any) => {
        acc[node.id] = {
          x: node.position?.x || 0,
          y: node.position?.y || 0,
          style: node.style
        }
        return acc
      }, {} as Record<string, any>)
    }

    return {
      name: frontendConfig.name,
      description: frontendConfig.description,
      app_id: frontendConfig.app_id,
      version: frontendConfig.version,
      nodes: frontendConfig.nodes.map((node: any) => this.frontendNodeToBackend(node)),
      edges: frontendConfig.edges.map((edge: any) => this.frontendEdgeToBackend(edge)),
      canvas: JSON.stringify(canvasData)
    }
  }

  /**
   * 将后端WorkflowConfigAddRequest转换为前端WorkflowConfig
   */
  static toFrontendConfig(backendRequest: any, id?: string): WorkflowConfig {
    // 解析canvas数据
    let canvasData: any = {
      viewport: { x: 0, y: 0, zoom: 1 },
      settings: {
        snapToGrid: true,
        gridSize: 15,
        showGrid: true,
        showMinimap: true
      },
      nodePositions: {}
    }

    if (backendRequest.canvas) {
      try {
        canvasData = JSON.parse(backendRequest.canvas)
      } catch (error) {
        console.warn('Failed to parse canvas data:', error)
      }
    }

    // 转换节点，应用保存的位置信息
    const nodes = backendRequest.nodes.map((node: any, index: number) => {
      const frontendNode = this.backendNodeToFrontend(node, index)
      
      // 如果有保存的位置信息，使用保存的位置
      if (canvasData.nodePositions && canvasData.nodePositions[node.id]) {
        const savedPosition = canvasData.nodePositions[node.id]
        frontendNode.position = { x: savedPosition.x, y: savedPosition.y }
        if (savedPosition.style) {
          frontendNode.style = savedPosition.style
        }
      }
      
      return frontendNode
    })

    return {
      id,
      name: backendRequest.name,
      description: backendRequest.description,
      app_id: backendRequest.app_id,
      version: backendRequest.version,
      nodes: nodes as any,
      edges: backendRequest.edges.map((edge: any) => this.backendEdgeToFrontend(edge)),
      viewport: canvasData.viewport || { x: 0, y: 0, zoom: 1 },
      settings: canvasData.settings || {
        snapToGrid: true,
        gridSize: 15,
        showGrid: true,
        showMinimap: true
      }
    }
  }

  /**
   * 前端节点转后端节点
   */
  private static frontendNodeToBackend(frontendNode: WorkflowNode): Node {
    return {
      id: frontendNode.id,
      name: frontendNode.data.name,
      desc: frontendNode.data.desc,
      type: frontendNode.type,
      config: frontendNode.data.config
    }
  }

  /**
   * 后端节点转前端节点
   */
  private static backendNodeToFrontend(backendNode: Node, index: number = 0): WorkflowNode {
    return {
      id: backendNode.id,
      type: backendNode.type,
      position: { 
        x: 100 + (index % 3) * 300, 
        y: 100 + Math.floor(index / 3) * 200 
      },
      data: {
        name: backendNode.name,
        desc: backendNode.desc,
        type: backendNode.type,  // ✅ 重要：data.type 用于 CustomNode
        config: backendNode.config,
        label: backendNode.name // 前端显示用
      }
    }
  }

  /**
   * 前端边转后端边
   */
  private static frontendEdgeToBackend(frontendEdge: WorkflowEdge): Edge {
    // ✅ 保存 sourceHandle 和 targetHandle，后端使用 source_handle 和 target_handle 字段
    const backendEdge = {
      id: frontendEdge.id,
      source: frontendEdge.source,
      target: frontendEdge.target,
      source_handle: frontendEdge.sourceHandle || `${frontendEdge.source}-output`,
      target_handle: frontendEdge.targetHandle || `${frontendEdge.target}-input`
    }
    
    return backendEdge
  }

  /**
   * 后端边转前端边
   */
  private static backendEdgeToFrontend(backendEdge: Edge): WorkflowEdge {
    // ✅ 根据节点ID生成正确的 sourceHandle 和 targetHandle
    // sourceHandle 格式：{source}-output 或 {source}-output-{index}（Judge节点）
    // targetHandle 格式：{target}-input
    const sourceHandle = backendEdge.source_handle || `${backendEdge.source}-output`
    const targetHandle = backendEdge.target_handle || `${backendEdge.target}-input`
    
    const frontendEdge = {
      id: backendEdge.id,
      source: backendEdge.source,
      target: backendEdge.target,
      sourceHandle: sourceHandle,
      targetHandle: targetHandle
    }
    
    return frontendEdge
  }

  /**
   * 创建输入参数
   */
  static createInputParam(
    key: string, 
    type: string, 
    valueFrom: string,
    value?: any,
    options?: Partial<InputParam>
  ): InputParam {
    return {
      key,
      type,
      value_from: valueFrom,
      value,
      desc: options?.desc,
      required: options?.required ?? false,
      default_value: options?.default_value
    }
  }

  /**
   * 创建输出参数
   */
  static createOutputParam(
    key: string,
    type: string,
    options?: Partial<OutputParam>
  ): OutputParam {
    return {
      key,
      type,
      desc: options?.desc,
      value: options?.value,
      value_from: options?.value_from,
      required: options?.required ?? false,
      default_value: options?.default_value,
      properties: options?.properties
    }
  }

  /**
   * 创建节点配置
   */
  static createNodeConfig(
    inputParams?: InputParam[],
    outputParams?: OutputParam[],
    nodeParam?: Record<string, any>
  ): NodeCustomConfig {
    return {
      input_params: inputParams,
      output_params: outputParams,
      node_param: nodeParam
    }
  }

  /**
   * 验证工作流配置
   */
  static validateWorkflowConfig(config: WorkflowConfig): {
    isValid: boolean
    errors: string[]
  } {
    const errors: string[] = []

    // 基本信息验证
    if (!config.name?.trim()) {
      errors.push('工作流名称不能为空')
    }

    if (!config.version?.trim()) {
      errors.push('版本号不能为空')
    }

    // 节点验证
    if (!config.nodes || config.nodes.length === 0) {
      errors.push('至少需要一个节点')
    } else {
      // 检查节点ID唯一性
      const nodeIds = config.nodes.map(n => n.id)
      const duplicateIds = nodeIds.filter((id, index) => nodeIds.indexOf(id) !== index)
      if (duplicateIds.length > 0) {
        errors.push(`节点ID重复: ${duplicateIds.join(', ')}`)
      }

      // 检查节点名称
      config.nodes.forEach((node: any, index: number) => {
        if (!node.data?.name?.trim()) {
          errors.push(`节点${index + 1}的名称不能为空`)
        }
      })
    }

    // 边验证
    if (config.edges && config.edges.length > 0) {
      // 检查边ID唯一性
      const edgeIds = config.edges.map(e => e.id)
      const duplicateEdgeIds = edgeIds.filter((id, index) => edgeIds.indexOf(id) !== index)
      if (duplicateEdgeIds.length > 0) {
        errors.push(`连接线ID重复: ${duplicateEdgeIds.join(', ')}`)
      }

      // 检查边的节点引用
      const nodeIds = new Set(config.nodes.map((n: any) => n.id))
      config.edges.forEach((edge: any, index: number) => {
        if (!nodeIds.has(edge.source)) {
          errors.push(`连接线${index + 1}的起点节点${edge.source}不存在`)
        }
        if (!nodeIds.has(edge.target)) {
          errors.push(`连接线${index + 1}的终点节点${edge.target}不存在`)
        }
      })
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  /**
   * 节点计数器，用于生成简短的节点ID
   */
  private static nodeCounters: Record<string, number> = {}

  /**
   * 生成节点ID（简化版本）
   * 格式：NodeType_Counter，例如 Start_1, TextGen_1, TextGen_2
   */
  static generateNodeId(type: string): string {
    if (!this.nodeCounters[type]) {
      this.nodeCounters[type] = 0
    }
    this.nodeCounters[type]++
    return `${type}_${this.nodeCounters[type]}`
  }

  /**
   * 重置节点计数器
   */
  static resetNodeCounters(): void {
    this.nodeCounters = {}
  }

  /**
   * 同步节点计数器（从现有节点中恢复计数器状态）
   */
  static syncNodeCounters(nodes: any[]): void {
    this.nodeCounters = {}
    nodes.forEach((node: any) => {
      const match = node.id.match(/^(.+)_(\d+)$/)
      if (match) {
        const [, type, counter] = match
        const num = parseInt(counter, 10)
        if (!this.nodeCounters[type] || this.nodeCounters[type] < num) {
          this.nodeCounters[type] = num
        }
      }
    })
  }

  /**
   * 生成边ID
   */
  static generateEdgeId(source: string, target: string): string {
    return `${source}_to_${target}`
  }
}
