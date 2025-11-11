/**
 * 知识库管理API
 */

import { get, post, put, del, upload } from '@/utils/request'
import type {
  KnowledgeBase,
  KnowledgeBaseAddRequest,
  KnowledgeBaseUpdateRequest,
  KnowledgeBasePageRequest,
  KnowledgeVector,
  KnowledgeVectorAddRequest,
  KnowledgeVectorUpdateRequest,
  KnowledgeVectorPageRequest,
  PageResult
} from '@/types/knowledge'

/**
 * 知识库管理API
 */
export const KnowledgeBaseAPI = {
  /**
   * 新增知识库
   */
  add(data: KnowledgeBaseAddRequest) {
    return post<number>('/api/knowledge-base/add', data)
  },

  /**
   * 更新知识库
   */
  update(data: KnowledgeBaseUpdateRequest) {
    return put<boolean>('/api/knowledge-base/update', data)
  },

  /**
   * 删除知识库
   */
  delete(id: number) {
    return del<boolean>(`/api/knowledge-base/delete/${id}`)
  },

  /**
   * 查询知识库详情
   */
  get(id: number) {
    return get<KnowledgeBase>(`/api/knowledge-base/get/${id}`)
  },

  /**
   * 分页查询知识库
   */
  page(data: KnowledgeBasePageRequest) {
    return post<PageResult<KnowledgeBase>>('/api/knowledge-base/page', data)
  }
}

/**
 * 知识向量管理API
 */
export const KnowledgeVectorAPI = {
  /**
   * 新增向量
   */
  add(data: KnowledgeVectorAddRequest) {
    return post<string>('/api/knowledge-vector/add', data)
  },

  /**
   * 更新向量
   */
  update(data: KnowledgeVectorUpdateRequest) {
    return put<boolean>('/api/knowledge-vector/update', data)
  },

  /**
   * 删除向量
   */
  delete(id: string) {
    return del<boolean>(`/api/knowledge-vector/delete/${id}`)
  },

  /**
   * 查询向量详情
   */
  get(id: string) {
    return get<KnowledgeVector>(`/api/knowledge-vector/get/${id}`)
  },

  /**
   * 分页查询向量
   */
  page(data: KnowledgeVectorPageRequest) {
    return post<PageResult<KnowledgeVector>>('/api/knowledge-vector/page', data)
  },

  /**
   * 上传文件到知识库
   */
  uploadFile(knowledgeBaseId: number, file: File) {
    const formData = new FormData()
    formData.append('knowledgeBaseId', String(knowledgeBaseId))
    formData.append('file', file)
    return upload<boolean>('/api/knowledge-vector/upload-file', formData)
  },

  /**
   * 相似性搜索
   */
  similaritySearch(knowledgeBaseId: number, query: string, topK: number = 5) {
    return get<KnowledgeVector[]>('/api/knowledge-vector/similarity-search', {
      knowledgeBaseId,
      query,
      topK
    })
  }
}

