/**
 * 知识库相关类型定义
 */

/**
 * 知识库
 */
export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  userId?: number
  status: number
  vectorCount: number
  createTime: string
  updateTime: string
}

/**
 * 新增知识库请求
 */
export interface KnowledgeBaseAddRequest {
  name: string
  description?: string
  status?: number
}

/**
 * 更新知识库请求
 */
export interface KnowledgeBaseUpdateRequest {
  id: number
  name?: string
  description?: string
  status?: number
}

/**
 * 知识库分页查询请求
 */
export interface KnowledgeBasePageRequest {
  pageNum: number
  pageSize: number
  name?: string
  status?: number
  userId?: number
}

/**
 * 知识向量
 */
export interface KnowledgeVector {
  id: string
  knowledgeBaseId: number
  content: string
  metadata?: string
  fileName?: string
  fileType?: string
  createTime: string
  updateTime: string
}

/**
 * 新增知识向量请求
 */
export interface KnowledgeVectorAddRequest {
  knowledgeBaseId: number
  content: string
  metadata?: string
  fileName?: string
  fileType?: string
}

/**
 * 更新知识向量请求
 */
export interface KnowledgeVectorUpdateRequest {
  id: string
  content?: string
  metadata?: string
  fileName?: string
  fileType?: string
}

/**
 * 知识向量分页查询请求
 */
export interface KnowledgeVectorPageRequest {
  pageNum: number
  pageSize: number
  knowledgeBaseId: number
  fileName?: string
  fileType?: string
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

