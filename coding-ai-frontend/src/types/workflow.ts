import type { Node as VueFlowNode } from '@vue-flow/core'

// 导入节点配置接口
export * from './nodeConfigs'

// ============ 后端接口类型定义 ============

// 值来源枚举
export enum ValueFromEnum {
  INPUT = 'INPUT',     // 输入
  INFER = 'INFER'      // 推断
}

// 通用参数类
export interface CommonParam {
  key: string                    // 参数名称
  type: string                   // 参数类型
  desc?: string                  // 参数描述
  value?: any                    // 参数值
  value_from?: string            // 值的来源
  required?: boolean             // 是否必需
  default_value?: any            // 默认值
}

// 输入参数
export interface InputParam extends CommonParam {
  // 继承CommonParam的所有属性
}

// 输出参数
export interface OutputParam extends CommonParam {
  properties?: OutputParam[]     // 子属性列表
}

// 节点自定义配置
export interface NodeCustomConfig {
  input_params?: InputParam[]    // 入参配置
  output_params?: OutputParam[]  // 出参配置
  node_param?: Record<string, any> // 节点配置
}

// 后端节点模型
export interface Node {
  id: string                     // 节点ID
  name: string                   // 节点名称
  desc?: string                  // 节点描述
  type: string                   // 节点类型
  config?: NodeCustomConfig      // 节点配置
}

// 后端边模型
export interface Edge {
  id: string                     // 边ID
  source: string                 // 起点节点ID
  source_handle?: string         // 起点连接句柄
  target: string                 // 终点节点ID
  target_handle?: string         // 终点连接句柄
}

// 工作流配置添加请求
export interface WorkflowConfigAddRequest {
  name: string                   // 工作流名称
  description?: string           // 工作流描述
  app_id?: number               // 所属应用/项目ID
  version: string               // 版本号
  nodes: Node[]                 // 节点列表
  edges: Edge[]                 // 边列表
  canvas?: string               // 前端画布数据(JSON字符串)
}

// 工作流配置更新请求
export interface WorkflowConfigUpdateRequest extends WorkflowConfigAddRequest {
  id: string                    // 工作流定义唯一ID
}

// ============ 前端扩展类型定义 ============

// 前端工作流节点 (继承Vue Flow Node + 后端Node)
export interface WorkflowNode extends VueFlowNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: {
    // 后端节点数据
    name: string
    desc?: string
    config?: NodeCustomConfig
    // 前端扩展数据
    label?: string
    [key: string]: any
  }
  style?: Record<string, any>
}

// 前端工作流边 (继承Vue Flow Edge + 后端Edge)
export interface WorkflowEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
  type?: string
  data?: {
    label?: string
    [key: string]: any
  }
  style?: Record<string, any>
}

// 节点类型枚举（匹配后端NodeTypeEnum）
export enum NodeType {
  START = 'Start',                    // 开始节点
  LLM = 'TextGen',                   // 大语言模型节点
  MCP = 'MCP',                       // MCP节点
  IMG_GEN = 'ImgGen',                // 图像生成节点
  VIDEO_GEN = 'VideoGen',            // 视频生成节点
  MUSIC_GEN = 'MusicGen',            // 音乐生成节点
  SCRIPT = 'Script',                 // 脚本执行节点
  EMAIL = 'Email',                   // 发送邮件执行节点
  JUDGE = 'Judge',                   // 判断节点
  END = 'End',                       // 结束节点
  OUTPUT = 'Output'                 // 输出节点
}

// 节点类型描述映射
export const NodeTypeDescriptions: Record<NodeType, string> = {
  [NodeType.START]: '开始节点',
  [NodeType.LLM]: '大语言模型节点',
  [NodeType.MCP]: 'MCP节点',
  [NodeType.IMG_GEN]: '图像生成节点',
  [NodeType.VIDEO_GEN]: '视频生成节点',
  [NodeType.MUSIC_GEN]: '音乐生成节点',
  [NodeType.SCRIPT]: '脚本执行节点',
  [NodeType.EMAIL]: '发送邮件执行节点',
  [NodeType.JUDGE]: '判断节点',
  [NodeType.END]: '结束节点',
  [NodeType.OUTPUT]: '输出节点'
}

// 连接点位置
export enum HandlePosition {
  TOP = 'top',
  RIGHT = 'right',
  BOTTOM = 'bottom',
  LEFT = 'left'
}

// 前端工作流配置 (包含Vue Flow需要的额外字段)
export interface WorkflowConfig extends WorkflowConfigAddRequest {
  id?: string                    // 前端临时ID
  viewport?: {
    x: number
    y: number
    zoom: number
  }
  settings?: {
    snapToGrid?: boolean
    gridSize?: number
    showGrid?: boolean
    showMinimap?: boolean
    [key: string]: any
  }
}

// 节点配置模板
export interface NodeTemplate {
  type: NodeType
  label: string
  description: string
  icon?: string
  category: string
  defaultConfig: Record<string, any>
  inputs?: Array<{
    name: string
    type: string
    required: boolean
    description?: string
  }>
  outputs?: Array<{
    name: string
    type: string
    description?: string
  }>
}

// 工作流执行状态
export enum ExecutionStatus {
  IDLE = 'idle',
  RUNNING = 'running',
  PAUSED = 'paused',
  COMPLETED = 'completed',
  FAILED = 'failed',
  CANCELLED = 'cancelled'
}

// 工作流执行结果
export interface ExecutionResult {
  id: string
  workflowId: string
  status: ExecutionStatus
  startTime: Date
  endTime?: Date
  duration?: number
  logs: Array<{
    timestamp: Date
    nodeId: string
    level: 'info' | 'warn' | 'error'
    message: string
    data?: any
  }>
  outputs?: Record<string, any>
  error?: {
    message: string
    stack?: string
    nodeId?: string
  }
}
