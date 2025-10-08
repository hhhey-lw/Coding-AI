/**
 * HTTP 请求工具 - 带自动Token刷新
 */

import { AuthAPI } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

// API 基础配置
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 是否正在刷新Token
let isRefreshing = false

// 等待刷新Token的请求队列
let refreshSubscribers: Array<(token: string) => void> = []

/**
 * 订阅Token刷新
 */
function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

/**
 * 通知所有订阅者Token已刷新
 */
function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach(callback => callback(token))
  refreshSubscribers = []
}

/**
 * 刷新Access Token
 */
async function refreshAccessToken(): Promise<string | null> {
  const authStore = useAuthStore()
  const refreshToken = authStore.getRefreshToken()
  
  if (!refreshToken) {
    console.error('❌ 没有Refresh Token，无法刷新')
    return null
  }

  try {
    console.log('🔄 尝试刷新Access Token...')
    const response = await AuthAPI.refreshToken({ refreshToken })
    
    if (response.code === 1 && response.data) {
      // ✅ 更新 Access Token 和 Refresh Token（后端会返回新的 Refresh Token）
      authStore.updateAccessToken(response.data.accessToken)
      
      // ✅ 同时更新 Refresh Token（重要！后端会返回新的 Refresh Token）
      if (response.data.refreshToken) {
        authStore.updateRefreshToken(response.data.refreshToken)
      }
      
      console.log('✅ Access Token 和 Refresh Token 刷新成功')
      console.log('新 Access Token:', response.data.accessToken.substring(0, 30) + '...')
      console.log('新 Refresh Token:', response.data.refreshToken.substring(0, 30) + '...')
      return response.data.accessToken
    } else {
      // code === 0 表示失败（Refresh Token 可能已过期）
      console.error('❌ Refresh Token刷新失败:', response.message)
      return null
    }
  } catch (error) {
    console.error('❌ Token刷新请求异常:', error)
    return null
  }
}

/**
 * 通用请求方法
 */
export async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<{ data: T; success: boolean; message?: string; code?: number; fail?: boolean }> {
  
  const authStore = useAuthStore()
  
  // 添加Authorization header（格式：Bearer {token}）
  const accessToken = authStore.getAccessToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers as Record<string, string>
  }
  
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }
  
  try {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers
    })

    console.log(`API响应状态: ${response.status} ${response.statusText}`)

    // 处理401未授权错误
    if (response.status === 401) {
      console.log('收到401错误，尝试刷新Token')
      
      // 如果正在刷新Token，等待刷新完成
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh(async (newToken: string) => {
            // Token刷新完成，重试请求
            headers['Authorization'] = `Bearer ${newToken}`
            try {
              const retryResponse = await fetch(`${API_BASE_URL}${url}`, {
                ...options,
                headers
              })
              const retryResult = await retryResponse.json()
              resolve(retryResult)
            } catch (error) {
              reject(error)
            }
          })
        })
      }

      // 开始刷新Token
      isRefreshing = true

      const newToken = await refreshAccessToken()
      
      if (newToken) {
        // Token刷新成功
        isRefreshing = false
        onTokenRefreshed(newToken)
        
        console.log('✅ Token自动刷新成功，重试原请求')
        
        // 重试原请求
        headers['Authorization'] = `Bearer ${newToken}`
        const retryResponse = await fetch(`${API_BASE_URL}${url}`, {
          ...options,
          headers
        })
        const retryResult = await retryResponse.json()
        return retryResult
      } else {
        // Token刷新失败，清除认证信息并跳转到登录页
        isRefreshing = false
        
        console.error('❌ Token刷新失败，跳转到登录页')
        
        const authStore = useAuthStore()
        authStore.clearAuth()
        
        // 显示错误提示
        ElMessage({
          type: 'warning',
          message: 'Token已过期，请重新登录',
          duration: 2000,
          showClose: true
        })
        
        // 延迟跳转，让用户看到提示
        setTimeout(() => {
          router.push('/login')
        }, 1000)
        
        throw new Error('Token已过期，请重新登录')
      }
    }

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    return result
  } catch (error) {
    console.error('请求失败:', error)
    throw error
  }
}

/**
 * GET 请求
 */
export function get<T>(url: string, params?: Record<string, any>) {
  const searchParams = new URLSearchParams()
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.set(key, String(value))
      }
    })
  }
  
  const queryString = searchParams.toString()
  const fullUrl = queryString ? `${url}?${queryString}` : url
  
  return request<T>(fullUrl, { method: 'GET' })
}

/**
 * POST 请求
 */
export function post<T>(url: string, data?: any) {
  return request<T>(url, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined
  })
}

/**
 * PUT 请求
 */
export function put<T>(url: string, data?: any) {
  return request<T>(url, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined
  })
}

/**
 * DELETE 请求
 */
export function del<T>(url: string) {
  return request<T>(url, { method: 'DELETE' })
}

/**
 * 上传文件（支持 FormData）
 */
export async function upload<T>(url: string, formData: FormData): Promise<{ data: T; success: boolean; message?: string; code?: number; fail?: boolean }> {
  const authStore = useAuthStore()
  const accessToken = authStore.getAccessToken()
  
  // ⚠️ 不设置 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary
  const headers: Record<string, string> = {}
  
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  console.log(`API上传: POST ${API_BASE_URL}${url}`)
  
  try {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'POST',
      headers,
      body: formData
    })

    console.log(`API响应状态: ${response.status} ${response.statusText}`)

    // 处理401未授权错误
    if (response.status === 401) {
      console.log('收到401错误，尝试刷新Token')
      
      // 如果正在刷新Token，等待刷新完成
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh(async (newToken: string) => {
            // Token刷新完成，重试请求
            headers['Authorization'] = `Bearer ${newToken}`
            try {
              const retryResponse = await fetch(`${API_BASE_URL}${url}`, {
                method: 'POST',
                headers,
                body: formData
              })
              const retryResult = await retryResponse.json()
              resolve(retryResult)
            } catch (error) {
              reject(error)
            }
          })
        })
      }

      // 开始刷新Token
      isRefreshing = true

      const newToken = await refreshAccessToken()
      
      if (newToken) {
        // Token刷新成功
        isRefreshing = false
        onTokenRefreshed(newToken)
        
        console.log('✅ Token自动刷新成功，重试上传请求')
        
        // 重试原请求
        headers['Authorization'] = `Bearer ${newToken}`
        const retryResponse = await fetch(`${API_BASE_URL}${url}`, {
          method: 'POST',
          headers,
          body: formData
        })
        const retryResult = await retryResponse.json()
        return retryResult
      } else {
        // Token刷新失败，清除认证信息并跳转到登录页
        isRefreshing = false
        
        console.error('❌ Token刷新失败，跳转到登录页')
        
        const authStore = useAuthStore()
        authStore.clearAuth()
        
        ElMessage({
          type: 'warning',
          message: 'Token已过期，请重新登录',
          duration: 2000,
          showClose: true
        })
        
        setTimeout(() => {
          router.push('/login')
        }, 1000)
        
        throw new Error('Token已过期，请重新登录')
      }
    }

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    console.log('API响应数据:', result)
    return result
  } catch (error) {
    console.error('上传请求失败:', error)
    throw error
  }
}

export default {
  request,
  get,
  post,
  put,
  delete: del,
  upload
}

