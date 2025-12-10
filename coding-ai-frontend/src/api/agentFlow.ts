import { post, get } from '@/utils/request'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface AgentFlowConfigRequest {
  id?: number | string
  name: string
  description?: string
  status?: number
  nodes: any[]
  edges: any[]
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
   * 保存或更新 AgentFlow
   */
  static async saveAgentFlow(data: AgentFlowConfigRequest) {
    return post<number>('/agent-flow/save', data)
  }

  /**
   * 根据ID查询 AgentFlow
   */
  static async getAgentFlow(id: number | string) {
    return get<AgentFlowConfigRequest>(`/agent-flow/${id}`)
  }

  /**
   * 获取流式执行的URL
   */
  static getExecuteStreamUrl(flowId: number | string, prompt: string) {
    // 使用配置的 API_BASE_URL
    return `${API_BASE_URL}/agent-flow/execute/stream?flowId=${flowId}&prompt=${encodeURIComponent(prompt)}`
  }
}
