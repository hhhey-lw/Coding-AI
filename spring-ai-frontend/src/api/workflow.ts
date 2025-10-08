import type { WorkflowConfigAddRequest, WorkflowConfigUpdateRequest } from '@/types/workflow'
import { get, post, put, upload } from '@/utils/request'

// 文件上传相关类型定义
export interface FileUploadResponse {
  fileName: string
  fileUrl: string
  fileSize: number
  contentType: string
  uploadTime: number
}

export interface FileUploadApiResponse {
  code: number
  message: string
  data: FileUploadResponse
  success: boolean
  fail: boolean
}

// MCP相关类型定义
export interface McpToolParam {
  key: string
  type: string
  desc: string
}

export interface McpServer {
  server_name: string
  server_code: string
  tool_name: string
  tool_params: McpToolParam[]
  return_description: string
}

export interface McpListResponse {
  code: number
  message: string
  data: McpServer[]
  success: boolean
  fail: boolean
}

// 模型相关类型定义
export interface ModelInfo {
  provider: string        // 提供商代码
  providerName: string    // 提供商名称
  modelType: string       // 模型类型
  modelId: string         // 模型标识
}

export interface ModelListResponse {
  code: number
  message: string
  data: ModelInfo[]
  success: boolean
  fail: boolean
}

export type ModelType = 'TextGen' | 'ImageGen' | 'AudioGen' | 'VideoGen' | 'MusicGen'

// 通用响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 工作流列表相关类型
export interface WorkflowConfigVO {
  id: string
  name: string
  description: string
  appId: string
  version: string
  nodes: string  // JSON字符串
  edges: string  // JSON字符串
  canvas: string // JSON字符串
  creator: string
  createTime: string
  updateTime: string
  status: number  // 1-启用，0-禁用
}

export interface PageVO<T> {
  pageNum: number
  pageSize: number
  total: string
  pages: number
  list: T[]
}

export interface WorkflowListParams {
  pageNum?: number
  pageSize?: number
  workflowName?: string
  status?: number
}

// API 路径统一管理
const API_PATHS = {
  // 工作流管理
  WORKFLOW: {
    ADD: '/ai/workflow/add',
    UPDATE: '/ai/workflow/update',
    UPDATE_STATUS: '/ai/workflow/updateStatus',
    GET: '/ai/workflow/{id}',
    MY_LIST: '/ai/workflow/my/list',
    RUN: '/ai/workflow/run/{workflowId}',
    RESULT: '/ai/workflow/result/{workflowInstanceId}'
  },
  // 模型管理
  MODEL: {
    LIST: '/model/list'
  },
  // MCP服务管理
  MCP: {
    LIST: '/mcp/list'
  },
  // 文件管理
  FILE: {
    UPLOAD_IMAGE: '/file/upload/image'
  }
}

/**
 * 工作流 API 接口
 */
export class WorkflowAPI {
  
  /**
   * 创建工作流
   */
  static async createWorkflow(workflowRequest: WorkflowConfigAddRequest) {
    return post<{ data: number }>(API_PATHS.WORKFLOW.ADD, workflowRequest)
  }

  /**
   * 更新工作流
   */
  static async updateWorkflow(id: string, workflowRequest: WorkflowConfigAddRequest) {
    // 构建更新请求，确保包含 id 字段
    const updateRequest: WorkflowConfigUpdateRequest = {
      ...workflowRequest,
      id: id
    }
    console.log('更新工作流请求数据:', updateRequest)
    
    return put<{ data: boolean }>(API_PATHS.WORKFLOW.UPDATE, updateRequest)
  }

  /**
   * 获取工作流详情
   * 返回工作流的完整配置信息
   */
  static async getWorkflow(id: string) {
    const url = API_PATHS.WORKFLOW.GET.replace('{id}', id)
    console.log('查询工作流详情:', url)
    return get<WorkflowConfigVO>(url)
  }

  /**
   * 更新工作流状态（删除实际上是禁用）
   */
  static async updateWorkflowStatus(id: string, status: number) {
    return put<ApiResponse<any>>(API_PATHS.WORKFLOW.UPDATE_STATUS, {
      id,
      status
    })
  }

  /**
   * 删除工作流（实际调用更新状态接口，设置为禁用）
   */
  static async deleteWorkflow(id: string) {
    return this.updateWorkflowStatus(id, 0)
  }

  /**
   * 获取我的工作流列表（分页）
   */
  static async getMyWorkflowList(params?: WorkflowListParams) {
    return get<PageVO<WorkflowConfigVO>>(API_PATHS.WORKFLOW.MY_LIST, params)
  }

  /**
   * 执行工作流
   */
  static async executeWorkflow(workflowId: string, inputParams?: Record<string, any>) {
    const url = API_PATHS.WORKFLOW.RUN.replace('{workflowId}', workflowId)
    return post<{ data: number }>(url, inputParams || {})
  }

  /**
   * 获取工作流运行结果
   */
  static async getWorkflowResult(workflowInstanceId: string) {
    const url = API_PATHS.WORKFLOW.RESULT.replace('{workflowInstanceId}', workflowInstanceId)
    return get<{ data: any }>(url)
  }

  /**
   * 停止工作流执行
   */
  static async stopExecution(executionId: string) {
    return post<{ success: boolean }>(`/workflow/execution/${executionId}/stop`)
  }

  /**
   * 验证工作流配置
   */
  static async validateWorkflow(workflowRequest: WorkflowConfigAddRequest) {
    return post<{
      isValid: boolean
      errors: string[]
      warnings: string[]
    }>('/workflow/validate', workflowRequest)
  }

  /**
   * 获取节点类型列表
   */
  static async getNodeTypes() {
    return get<Array<{
      type: string
      name: string
      description: string
      category: string
      icon?: string
      inputSchema: Record<string, any>
      outputSchema: Record<string, any>
    }>>('/workflow/node-types')
  }

  /**
   * 获取MCP服务列表
   */
  static async getMcpServers(): Promise<McpListResponse> {
    return get<McpServer[]>(API_PATHS.MCP.LIST) as Promise<McpListResponse>
  }

  /**
   * 获取模型列表
   */
  static async getModelList(modelType: ModelType): Promise<ModelListResponse> {
    return get<ModelInfo[]>(API_PATHS.MODEL.LIST, { modelType }) as Promise<ModelListResponse>
  }

  /**
   * 上传图片文件
   */
  static async uploadImage(file: File): Promise<FileUploadApiResponse> {
    console.log(`开始上传图片: ${file.name}, 大小: ${file.size} bytes`)
    
    const formData = new FormData()
    formData.append('file', file)
    
    return upload<FileUploadResponse>(API_PATHS.FILE.UPLOAD_IMAGE, formData) as Promise<FileUploadApiResponse>
  }
}

// 导出默认实例
export default WorkflowAPI
