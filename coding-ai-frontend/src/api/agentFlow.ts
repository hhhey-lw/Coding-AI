import { post, get, del } from '@/utils/request'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface AgentFlowConfigRequest {
  id?: number | string
  name: string
  description?: string
  status?: number
  nodes: any[]
  edges: any[]
}

export interface AgentFlowConfigResponse {
  id: number
  name: string
  description?: string
  nodes: any[]
  edges: any[]
  status: number
  creatorId: number
  createTime: string
  updateTime: string
}

export interface AgentFlowPageResponse {
  records: AgentFlowConfigResponse[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ToolInfo {
  name: string
  description?: string
  params?: Record<string, string>
}

export class AgentFlowAPI {
  /**
   * 获取可用工具列表
   */
  static async getTools() {
    return get<ToolInfo[]>('/agent-flow/tools')
  }

  /**
   * 分页查询 AgentFlow 列表
   */
  static async page(params: { current?: number; size?: number; name?: string }) {
    return get<AgentFlowPageResponse>('/agent-flow/page', params)
  }

  /**
   * 保存或更新 AgentFlow
   */
  static async saveAgentFlow(data: AgentFlowConfigRequest) {
    return post<number>('/agent-flow/save', data)
  }

  /**
   * 根据ID查询 AgentFlow
   */
  static async getAgentFlow(id: number | string) {
    return get<AgentFlowConfigResponse>(`/agent-flow/${id}`)
  }

  /**
   * 删除 AgentFlow
   */
  static async deleteAgentFlow(id: number | string) {
    return del<boolean>(`/agent-flow/${id}`)
  }

  /**
   * 获取流式执行的URL
   */
  static getExecuteStreamUrl(flowId: number | string, prompt: string) {
    // 使用配置的 API_BASE_URL
    return `${API_BASE_URL}/agent-flow/execute/stream?flowId=${flowId}&prompt=${encodeURIComponent(prompt)}`
  }
}
