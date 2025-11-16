/**
 * 用户认证 API
 */

// 类型定义
export interface SendCodeRequest {
  email: string
}

export interface RegisterRequest {
  email: string
  password: string
  code: string
  userName?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  userId: number
  email: string
  userName: string
  userAvatar: string | null
  userRole: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: string | number  // 后端返回的是字符串 "7200"
  tokenType: string
}

export interface LogoutRequest {
  refreshToken: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success?: boolean
  fail?: boolean
}

// API 基础配置
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// API 路径
const API_PATHS = {
  SEND_CODE: '/user/send-code',
  REGISTER: '/user/register',
  LOGIN: '/user/login',
  REFRESH_TOKEN: '/user/refresh-token',
  LOGOUT: '/user/logout'
}

/**
 * 用户认证 API 类
 */
export class AuthAPI {
  
  /**
   * 发送邮箱验证码
   * 验证码有效期5分钟
   */
  static async sendCode(sendCodeRequest: SendCodeRequest): Promise<ApiResponse<null>> {
    console.log('发送验证码请求:', sendCodeRequest.email)
    
    const response = await fetch(`${API_BASE_URL}${API_PATHS.SEND_CODE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(sendCodeRequest),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    console.log('发送验证码响应:', result)
    return result
  }

  /**
   * 用户注册
   * 返回创建的用户ID
   */
  static async register(registerRequest: RegisterRequest): Promise<ApiResponse<number>> {
    console.log('注册请求:', registerRequest.email)
    
    const response = await fetch(`${API_BASE_URL}${API_PATHS.REGISTER}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerRequest),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    console.log('注册响应:', result)
    return result
  }
  
  /**
   * 用户登录
   */
  static async login(loginRequest: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    console.log('登录请求:', loginRequest.email)
    
    const response = await fetch(`${API_BASE_URL}${API_PATHS.LOGIN}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(loginRequest),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    console.log('登录响应:', result)
    return result
  }

  /**
   * 刷新Token
   */
  static async refreshToken(refreshTokenRequest: RefreshTokenRequest): Promise<ApiResponse<RefreshTokenResponse>> {
    console.log('刷新Token请求')
    
    const response = await fetch(`${API_BASE_URL}${API_PATHS.REFRESH_TOKEN}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(refreshTokenRequest),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    console.log('刷新Token响应:', result)
    return result
  }

  /**
   * 用户登出
   */
  static async logout(logoutRequest: LogoutRequest): Promise<ApiResponse<null>> {
    console.log('登出请求')
    
    const response = await fetch(`${API_BASE_URL}${API_PATHS.LOGOUT}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(logoutRequest),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    console.log('登出响应:', result)
    return result
  }
}

/**
 * Token 管理工具类
 */
export class TokenManager {
  
  /**
   * 保存登录信息
   */
  static saveLoginInfo(loginResponse: LoginResponse) {
    // Access Token 存储在 sessionStorage（关闭浏览器后失效）
    sessionStorage.setItem('accessToken', loginResponse.accessToken)
    
    // Refresh Token 存储在 localStorage（持久化7天）
    localStorage.setItem('refreshToken', loginResponse.refreshToken)
    
    // 保存用户信息
    localStorage.setItem('userInfo', JSON.stringify({
      userId: loginResponse.userId,
      email: loginResponse.email,
      userName: loginResponse.userName,
      userAvatar: loginResponse.userAvatar,
      userRole: loginResponse.userRole
    }))
    
    console.log('Token已保存:', {
      accessToken: loginResponse.accessToken.substring(0, 20) + '...',
      refreshToken: loginResponse.refreshToken.substring(0, 20) + '...',
      expiresIn: loginResponse.expiresIn
    })
  }

  /**
   * 获取Access Token
   */
  static getAccessToken(): string | null {
    return sessionStorage.getItem('accessToken')
  }

  /**
   * 获取Refresh Token
   */
  static getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken')
  }

  /**
   * 获取用户信息
   */
  static getUserInfo(): any | null {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        return JSON.parse(userInfoStr)
      } catch (error) {
        console.error('解析用户信息失败:', error)
        return null
      }
    }
    return null
  }

  /**
   * 更新Access Token
   */
  static updateAccessToken(accessToken: string) {
    sessionStorage.setItem('accessToken', accessToken)
    console.log('Access Token已更新')
  }

  /**
   * 清除所有Token和用户信息
   */
  static clearAll() {
    sessionStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    console.log('Token已清除')
  }

  /**
   * 检查是否已登录
   */
  static isLoggedIn(): boolean {
    return !!this.getRefreshToken()
  }
}

// 导出默认实例
export default AuthAPI

