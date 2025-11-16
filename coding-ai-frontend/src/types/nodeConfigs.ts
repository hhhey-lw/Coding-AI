/**
 * 节点配置接口定义
 * 基于后端文档定义每个节点类型的具体配置接口
 */

import type { NodeCustomConfig, InputParam, OutputParam } from './workflow'

// ============ 通用接口定义 ============

// JSON参数配置 (用于End节点)
export interface JsonParam {
  key: string              // 输出键名
  value: string            // 输出值 (支持模板变量)
  value_from: 'refer' | 'input'  // 值来源
  type: string             // 数据类型
}

// ============ 节点配置接口 ============

// 参数配置 (用于Start节点)
export interface ParamConfig {
  key: string       // 参数键名
  type: string      // 参数类型
  value: string     // 参数值
  value_from?: 'input' | 'refer'  // 值来源
}

// 1. 开始节点配置
export interface StartNodeConfig extends NodeCustomConfig {
  input_params: ParamConfig[]  // 用户自定义输入参数
  output_params: OutputParam[] // 输出参数（与输入参数相同）
  node_param: Record<string, any>  // 空对象
}

// 模型参数配置
export interface ModelParam {
  key: string       // 参数键名
  value: string     // 参数值
  enable: boolean   // 是否启用
}

// 模型配置
export interface ModelConfig {
  provider: string        // 供应商
  model_id: string       // 模型ID
  model_name: string     // 模型名称
  params: ModelParam[]   // 模型参数列表
}

// 2. LLM节点配置
export interface LLMNodeConfig extends NodeCustomConfig {
  node_param: {
    sys_prompt_content: string  // 系统提示词
    prompt_content: string      // 用户提示词 (支持${})
    model_config: ModelConfig   // 模型配置
  }
  output_params: OutputParam[]  // 输出：output (str)
}

// 3. MCP节点配置
export interface MCPNodeConfig extends NodeCustomConfig {
  input_params: InputParam[] // 动态输入参数
  node_param: {
    server_code: string     // MCP服务编码 (必填)
    server_name: string     // MCP服务名称 (必填)
    tool_name: string       // 工具名称 (必填)
  }
  output_params: OutputParam[] // 输出：result
}

// 4. 图像生成节点配置
export interface ImageGenNodeConfig extends NodeCustomConfig {
  node_param: {
    provider: string        // 供应商名称 (下拉框)
    model: string           // 模型名称 (下拉框)
    prompt: string          // 输入提示词 (支持${})
    imgSize: string         // 生成图像尺寸
    maxImages: number       // 最大组图数量
    imageUrls: string[]     // 参考图URL列表（可以是上传的URL或变量引用，两者互斥）
    watermark: boolean      // 是否启用水印
  }
  output_params: OutputParam[] // 输出：output (图片链接)
}

// 5. 视频生成节点配置
export interface VideoGenNodeConfig extends NodeCustomConfig {
  node_param: {
    provider: string        // 供应商名称 (下拉框)
    model_id: string        // 模型ID (下拉框)
    input_prompt: string    // 输入提示词 (支持${})
    resolution: string      // 视频分辨率 (480P, 720P, 1080P)
    duration: number        // 视频时长(秒)
    first_frame_image: string  // 首帧图像URL（可以是上传的URL或变量引用）
    tail_frame_image: string   // 尾帧图像URL（可以是上传的URL或变量引用）
  }
  output_params: OutputParam[] // 输出：video (视频URL), message (成功or失败)
}

// 6. 音乐生成节点配置
export interface MusicGenNodeConfig extends NodeCustomConfig {
  node_param: {
    provider: string        // 供应商名称 (下拉框)
    model: string           // 模型名称 (下拉框)
    prompt: string          // 输入提示词 (支持${})
    lyrics: string          // 歌词内容
  }
  output_params: OutputParam[] // 输出：output (mp3 URL)
}

// 7. 脚本执行节点配置
export interface ScriptNodeConfig extends NodeCustomConfig {
  node_param: {
    scriptType: 'javascript' | 'python'  // 脚本类型
    scriptContent: string   // 脚本内容
  }
  output_params: OutputParam[] // 输出参数（根据key从脚本执行结果中获取）
}

// 8. 邮件发送节点配置
export interface EmailNodeConfig extends NodeCustomConfig {
  node_param: {
    to: string              // 收件人邮箱 (支持${})
    from: string            // 发件人邮箱
    subject: string         // 邮件主题 (支持${})
    content: string         // 邮件内容 (支持${})
    html: boolean           // 是否为HTML格式
    authorization: string   // 发件人认证令牌/应用密码
  }
  output_params: OutputParam[] // 输出：output (发送成功or发送失败)
}

// 分支条件配置
export interface BranchCondition {
  left_key: {
    key: string
    type: string
    value: string  // 当为${xxx}时会去上下文取
  }
  operator: 'isNull' | 'isNotNull' | 'equals' | 'notEquals'  // 操作符
  right_value: string
}

// 分支配置
export interface Branch {
  id: string  // 下一个节点ID（目标节点ID）
  label: string  // 分支标签 (IF, ELSE IF, ELSE)
  conditions: BranchCondition[]  // IF条件配置（ELSE分支为空数组）
}

// 9. 判断节点配置 (分支节点)
export interface JudgeNodeConfig extends NodeCustomConfig {
  node_param: {
    branches: Branch[]  // 每个分支标识一个if-elseIf-else的一个分支
  }
}

// 11. 结束节点配置
export interface EndNodeConfig extends NodeCustomConfig {
  node_param: {
    output_type: 'text' | 'json'  // 输出类型 (必填)
    text_template?: string  // 文本模板 (output_type为text时必填)
    stream_switch?: boolean // 流式开关 (可选)
    json_params?: JsonParam[] // JSON参数配置 (output_type为json时必填)
  }
}

// ============ 节点配置类型映射 ============

export type NodeConfigMap = {
  'Start': StartNodeConfig
  'TextGen': LLMNodeConfig
  'MCP': MCPNodeConfig
  'ImgGen': ImageGenNodeConfig
  'VideoGen': VideoGenNodeConfig
  'MusicGen': MusicGenNodeConfig
  'Script': ScriptNodeConfig
  'Email': EmailNodeConfig
  'Judge': JudgeNodeConfig
  'End': EndNodeConfig
}

// 根据节点类型获取对应的配置类型
export type GetNodeConfig<T extends keyof NodeConfigMap> = NodeConfigMap[T]

// ============ 节点配置创建工厂 ============

export class NodeConfigFactory {
  
  /**
   * 创建开始节点配置
   */
  static createStartConfig(inputParams: ParamConfig[] = []): StartNodeConfig {
    // 如果没有参数，添加默认的input参数
    const params = inputParams.length === 0 
      ? [{ key: 'input', type: 'String', value: '', value_from: 'input' as const }]
      : inputParams
    
    // 输出参数与输入参数相同
    const outputParams = params.map(param => ({
      key: param.key,
      type: param.type,
      desc: `输出参数: ${param.key}`
    }))
    
    return {
      input_params: params,
      output_params: outputParams,
      node_param: {}
    }
  }

  /**
   * 创建LLM节点配置
   */
  static createLLMConfig(config: {
    sys_prompt_content: string
    prompt_content: string
    model_config: ModelConfig
  }): LLMNodeConfig {
    return {
      input_params: [],
      output_params: [
        { key: 'output', type: 'string', desc: 'LLM输出结果' }
      ],
      node_param: config
    }
  }

  /**
   * 创建MCP节点配置
   */
  static createMCPConfig(config: {
    server_code: string
    server_name: string
    tool_name: string
  }, inputParams: InputParam[] = []): MCPNodeConfig {
    return {
      input_params: inputParams,
      output_params: [
        { key: 'result', type: 'string', desc: 'MCP执行结果' }
      ],
      node_param: config
    }
  }

  /**
   * 创建图像生成节点配置
   */
  static createImageGenConfig(config: {
    provider: string
    model: string
    prompt: string
    imgSize: string
    maxImages: number
    imageUrls: string[]
    watermark: boolean
  }): ImageGenNodeConfig {
    return {
      input_params: [],
      output_params: [
        { key: 'output', type: 'string', desc: '图片链接' }
      ],
      node_param: config
    }
  }

  /**
   * 创建视频生成节点配置
   */
  static createVideoGenConfig(config: {
    provider: string
    model_id: string
    input_prompt: string
    resolution: string
    duration: number
    first_frame_image: string
    tail_frame_image: string
  }): VideoGenNodeConfig {
    return {
      input_params: [],
      output_params: [
        { key: 'video', type: 'string', desc: '视频URL' },
        { key: 'message', type: 'string', desc: '成功or失败' }
      ],
      node_param: config
    }
  }

  /**
   * 创建音乐生成节点配置
   */
  static createMusicGenConfig(config: {
    provider: string
    model: string
    prompt: string
    lyrics: string
  }): MusicGenNodeConfig {
    return {
      input_params: [],
      output_params: [
        { key: 'output', type: 'string', desc: 'mp3 URL' }
      ],
      node_param: config
    }
  }

  /**
   * 创建脚本执行节点配置
   */
  static createScriptConfig(config: {
    scriptType: 'javascript' | 'python'
    scriptContent: string
  }, outputParams: OutputParam[] = []): ScriptNodeConfig {
    return {
      input_params: [],
      output_params: outputParams,
      node_param: config
    }
  }

  /**
   * 创建邮件发送节点配置
   */
  static createEmailConfig(config: {
    to: string
    from: string
    subject: string
    content: string
    html: boolean
    authorization: string
  }): EmailNodeConfig {
    return {
      input_params: [],
      output_params: [
        { key: 'output', type: 'string', desc: '发送成功or发送失败' }
      ],
      node_param: config
    }
  }

  /**
   * 创建判断节点配置（分支节点）
   */
  static createJudgeConfig(branches: Branch[] = []): JudgeNodeConfig {
    return {
      input_params: [],
      output_params: [],
      node_param: {
        branches
      }
    }
  }

  /**
   * 创建结束节点配置
   */
  static createEndConfig(config: {
    output_type: 'text' | 'json'
    text_template?: string
    stream_switch?: boolean
    json_params?: JsonParam[]
  }): EndNodeConfig {
    return {
      input_params: [], // End节点不需要显示输入参数
      output_params: [],
      node_param: config
    }
  }
}

// ============ 节点配置验证 ============

export class NodeConfigValidator {
  
  /**
   * 验证节点配置
   */
  static validateNodeConfig(nodeType: string, config: NodeCustomConfig): {
    isValid: boolean
    errors: string[]
  } {
    const errors: string[] = []

    switch (nodeType) {
      case 'ImgGen':
        errors.push(...this.validateImageGenConfig(config as ImageGenNodeConfig))
        break
      case 'MCP':
        errors.push(...this.validateMCPConfig(config as MCPNodeConfig))
        break
      case 'MusicGen':
        errors.push(...this.validateMusicGenConfig(config as MusicGenNodeConfig))
        break
      case 'Script':
        errors.push(...this.validateScriptConfig(config as ScriptNodeConfig))
        break
      case 'Email':
        errors.push(...this.validateEmailConfig(config as EmailNodeConfig))
        break
      case 'VideoGen':
        errors.push(...this.validateVideoGenConfig(config as VideoGenNodeConfig))
        break
      case 'End':
        errors.push(...this.validateEndConfig(config as EndNodeConfig))
        break
      // 其他节点类型的验证...
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  private static validateImageGenConfig(config: ImageGenNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.provider) errors.push('provider is required')
    if (!config.node_param?.model) errors.push('model is required')
    if (!config.node_param?.prompt) errors.push('prompt is required')
    if (!config.node_param?.imgSize) errors.push('imgSize is required')
    return errors
  }

  private static validateMCPConfig(config: MCPNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.server_code) errors.push('server_code is required')
    if (!config.node_param?.tool_name) errors.push('tool_name is required')
    return errors
  }

  private static validateMusicGenConfig(config: MusicGenNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.provider) errors.push('provider is required')
    if (!config.node_param?.model) errors.push('model is required')
    if (!config.node_param?.prompt) errors.push('prompt is required')
    return errors
  }

  private static validateScriptConfig(config: ScriptNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.scriptType) errors.push('scriptType is required')
    if (!config.node_param?.scriptContent) errors.push('scriptContent is required')
    return errors
  }

  private static validateEmailConfig(config: EmailNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.to) errors.push('to is required')
    if (!config.node_param?.from) errors.push('from is required')
    if (!config.node_param?.subject) errors.push('subject is required')
    if (!config.node_param?.content) errors.push('content is required')
    if (!config.node_param?.authorization) errors.push('authorization is required')
    return errors
  }

  private static validateVideoGenConfig(config: VideoGenNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.provider) errors.push('provider is required')
    if (!config.node_param?.model_id) errors.push('model_id is required')
    if (!config.node_param?.input_prompt) errors.push('input_prompt is required')
    if (!config.node_param?.resolution) errors.push('resolution is required')
    if (!config.node_param?.duration) errors.push('duration is required')
    // first_frame_image 和 tail_frame_image 是可选的
    return errors
  }

  private static validateEndConfig(config: EndNodeConfig): string[] {
    const errors: string[] = []
    if (!config.node_param?.output_type) errors.push('output_type is required')
    if (config.node_param.output_type === 'text' && !config.node_param.text_template) {
      errors.push('text_template is required when output_type is text')
    }
    if (config.node_param.output_type === 'json' && !config.node_param.json_params?.length) {
      errors.push('json_params is required when output_type is json')
    }
    return errors
  }
}
