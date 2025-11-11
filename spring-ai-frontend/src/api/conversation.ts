/**
 * 聊天会话管理API
 */

import { get, post, del } from '@/utils/request'

/**
 * 聊天会话
 */
export interface ChatConversation {
  id: string
  userId?: string
  title: string
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 会话详情响应
 */
export interface ChatConversationDetailResponse {
  conversation: ChatConversation
  messages: SimpleChatMessage[]
}

/**
 * 简化的聊天消息
 */
export interface SimpleChatMessage {
  role: string
  content: string
  toolCalls?: ToolCall[]
  responses?: ToolResponse[]
}

/**
 * 工具调用
 */
export interface ToolCall {
  id: string
  name: string
  arguments: string
}

/**
 * 工具响应
 */
export interface ToolResponse {
  id: string
  name: string
  responseData: string
}

/**
 * 创建会话请求
 */
export interface ChatConversationCreateRequest {
  id?: string
  title: string
}

/**
 * 会话分页查询请求
 */
export interface ChatConversationPageRequest {
  pageNum: number
  pageSize: number
  status?: number
}

/**
 * 分页结果
 */
export interface PageVO<T> {
  list: T[]
  total: string
  pageNum: number
  pageSize: number
}

/**
 * 会话API
 */
export const ConversationAPI = {
  /**
   * 创建会话
   */
  createConversation(data: ChatConversationCreateRequest) {
    return post<string>('/ai/conversation/create', data)
  },

  /**
   * 分页查询会话列表
   */
  getConversationPage(params: ChatConversationPageRequest) {
    return get<PageVO<ChatConversation>>('/ai/conversation/page', params)
  },

  /**
   * 查询会话详情
   */
  getConversationDetail(id: string) {
    return get<ChatConversationDetailResponse>(`/ai/conversation/${id}`)
  },

  /**
   * 删除会话
   */
  deleteConversation(id: string) {
    return del<boolean>(`/ai/conversation/${id}`)
  }
}

