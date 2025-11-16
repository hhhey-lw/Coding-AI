/**
 * 认证状态管理 Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const userInfo = ref<{
    userId: number
    email: string
    userName: string
    userAvatar: string | null
    userRole: string
  } | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!refreshToken.value)
  const hasAccessToken = computed(() => !!accessToken.value)

  // Actions
  
  /**
   * 初始化：从 localStorage 和 sessionStorage 加载数据
   */
  function init() {
    // 加载 Access Token（sessionStorage）
    const storedAccessToken = sessionStorage.getItem('accessToken')
    if (storedAccessToken) {
      accessToken.value = storedAccessToken
    }

    // 加载 Refresh Token（localStorage）
    const storedRefreshToken = localStorage.getItem('refreshToken')
    if (storedRefreshToken) {
      refreshToken.value = storedRefreshToken
    }

    // 加载用户信息（localStorage）
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      try {
        userInfo.value = JSON.parse(storedUserInfo)
      } catch (error) {
        console.error('解析用户信息失败:', error)
      }
    }

    console.log('Auth Store 初始化:', {
      hasAccessToken: !!accessToken.value,
      hasRefreshToken: !!refreshToken.value,
      userInfo: userInfo.value
    })
  }

  /**
   * 保存登录信息
   */
  function setLoginInfo(loginResponse: LoginResponse) {
    // 保存 Access Token 到 sessionStorage
    accessToken.value = loginResponse.accessToken
    sessionStorage.setItem('accessToken', loginResponse.accessToken)

    // 保存 Refresh Token 到 localStorage
    refreshToken.value = loginResponse.refreshToken
    localStorage.setItem('refreshToken', loginResponse.refreshToken)

    // 保存用户信息到 localStorage
    userInfo.value = {
      userId: loginResponse.userId,
      email: loginResponse.email,
      userName: loginResponse.userName,
      userAvatar: loginResponse.userAvatar,
      userRole: loginResponse.userRole
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))

    console.log('登录信息已保存到 Store')
  }

  /**
   * 更新 Access Token
   */
  function updateAccessToken(newAccessToken: string) {
    accessToken.value = newAccessToken
    sessionStorage.setItem('accessToken', newAccessToken)
    console.log('Access Token 已更新')
  }

  /**
   * 更新 Refresh Token
   */
  function updateRefreshToken(newRefreshToken: string) {
    refreshToken.value = newRefreshToken
    localStorage.setItem('refreshToken', newRefreshToken)
    console.log('Refresh Token 已更新')
  }

  /**
   * 清除所有认证信息（登出）
   */
  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    userInfo.value = null

    sessionStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')

    console.log('认证信息已清除')
  }

  /**
   * 获取 Access Token
   */
  function getAccessToken(): string | null {
    return accessToken.value
  }

  /**
   * 获取 Refresh Token
   */
  function getRefreshToken(): string | null {
    return refreshToken.value
  }

  return {
    // State
    accessToken,
    refreshToken,
    userInfo,
    
    // Getters
    isLoggedIn,
    hasAccessToken,
    
    // Actions
    init,
    setLoginInfo,
    updateAccessToken,
    updateRefreshToken,
    clearAuth,
    getAccessToken,
    getRefreshToken
  }
})

