/**
 * 节点创建工具类
 * 基于具体的节点配置接口创建节点
 */

import type { 
  Node,
  // LLMNodeConfig,
  // MCPNodeConfig,
  // ImageGenNodeConfig,
  // VideoGenNodeConfig,
  // MusicGenNodeConfig,
  // ScriptNodeConfig,
  // EmailNodeConfig,
  // JudgeNodeConfig,
  // EndNodeConfig,
  InputParam,
  OutputParam,
  JsonParam,
  ModelConfig,
  ParamConfig,
  Branch
} from '@/types/workflow'
import { NodeConfigFactory } from '@/types/workflow'
import { WorkflowTransform } from './workflowTransform'

export class NodeCreator {
  
  /**
   * 创建开始节点
   */
  static createStartNode(options: {
    id?: string
    name?: string
    desc?: string
    inputParams?: ParamConfig[]
  } = {}): Node {
    const { id, name, desc, inputParams = [] } = options
    
    return {
      id: id || WorkflowTransform.generateNodeId('Start'),
      name: name || '开始节点',
      desc: desc || '工作流的入口节点',
      type: 'Start',
      config: NodeConfigFactory.createStartConfig(inputParams)
    }
  }

  /**
   * 创建LLM节点
   */
  static createLLMNode(options: {
    id?: string
    name?: string
    desc?: string
    sys_prompt_content: string
    prompt_content: string
    model_config: ModelConfig
  }): Node {
    const { id, name, desc, ...configParams } = options
    
    return {
      id: id || WorkflowTransform.generateNodeId('TextGen'),
      name: name || 'LLM处理',
      desc: desc || '大语言模型处理节点',
      type: 'TextGen',
      config: NodeConfigFactory.createLLMConfig(configParams)
    }
  }

  /**
   * 创建MCP节点
   */
  static createMCPNode(options: {
    id?: string
    name?: string
    desc?: string
    server_code: string
    server_name: string
    tool_name: string
    inputParams?: InputParam[]
  }): Node {
    const { id, name, desc, inputParams = [], ...configParams } = options

    return {
      id: id || WorkflowTransform.generateNodeId('MCP'),
      name: name || 'MCP处理',
      desc: desc || 'MCP协议处理节点',
      type: 'MCP',
      config: NodeConfigFactory.createMCPConfig(configParams, inputParams)
    }
  }

  /**
   * 创建图像生成节点
   */
  static createImageGenNode(options: {
    id?: string
    name?: string
    desc?: string
    provider: string
    model: string
    prompt: string
    imgSize: string
    maxImages?: number
    imageUrls?: string[]
    watermark?: boolean
  }): Node {
    const {
      id,
      name,
      desc,
      imageUrls = [],
      maxImages = 1,
      watermark = false,
      ...configParams
    } = options

    return {
      id: id || WorkflowTransform.generateNodeId('ImgGen'),
      name: name || '图像生成',
      desc: desc || '图像生成节点',
      type: 'ImgGen',
      config: NodeConfigFactory.createImageGenConfig({
        ...configParams,
        imageUrls,
        maxImages,
        watermark
      })
    }
  }

  /**
   * 创建视频生成节点
   */
  static createVideoGenNode(options: {
    id?: string
    name?: string
    desc?: string
    provider: string
    model_id: string
    input_prompt: string
    first_frame_image: string
    tail_frame_image?: string
    resolution: string
    duration: number
  }): Node {
    const { id, name, desc, tail_frame_image = '', ...configParams } = options
    
    return {
      id: id || WorkflowTransform.generateNodeId('VideoGen'),
      name: name || '视频生成',
      desc: desc || '视频生成节点',
      type: 'VideoGen',
      config: NodeConfigFactory.createVideoGenConfig({
        ...configParams,
        tail_frame_image
      })
    }
  }

  /**
   * 创建音乐生成节点
   */
  static createMusicGenNode(options: {
    id?: string
    name?: string
    desc?: string
    provider: string
    model: string
    prompt: string
    lyrics?: string
  }): Node {
    const { id, name, desc, lyrics = '', ...configParams } = options
    
    return {
      id: id || WorkflowTransform.generateNodeId('MusicGen'),
      name: name || '音乐生成',
      desc: desc || '音乐生成节点',
      type: 'MusicGen',
      config: NodeConfigFactory.createMusicGenConfig({
        ...configParams,
        lyrics
      })
    }
  }

  /**
   * 创建脚本执行节点
   */
  static createScriptNode(options: {
    id?: string
    name?: string
    desc?: string
    scriptType: 'javascript' | 'python'
    scriptContent: string
    outputParams?: OutputParam[]
  }): Node {
    const { id, name, desc, outputParams = [], ...configParams } = options

    return {
      id: id || WorkflowTransform.generateNodeId('Script'),
      name: name || '脚本执行',
      desc: desc || '脚本执行节点',
      type: 'Script',
      config: NodeConfigFactory.createScriptConfig(configParams, outputParams)
    }
  }

  /**
   * 创建邮件发送节点
   */
  static createEmailNode(options: {
    id?: string
    name?: string
    desc?: string
    to: string
    from: string
    subject: string
    content: string
    html?: boolean
    authorization: string
  }): Node {
    const { id, name, desc, html = false, ...configParams } = options

    return {
      id: id || WorkflowTransform.generateNodeId('Email'),
      name: name || '发送邮件',
      desc: desc || '邮件发送节点',
      type: 'Email',
      config: NodeConfigFactory.createEmailConfig({
        ...configParams,
        html
      })
    }
  }

  /**
   * 创建判断节点（分支节点）
   */
  static createJudgeNode(options: {
    id?: string
    name?: string
    desc?: string
    branches?: Branch[]
  } = {}): Node {
    const { id, name, desc, branches = [] } = options
    
    return {
      id: id || WorkflowTransform.generateNodeId('Judge'),
      name: name || '条件判断',
      desc: desc || '条件判断/分支节点',
      type: 'Judge',
      config: NodeConfigFactory.createJudgeConfig(branches)
    }
  }

  /**
   * 创建结束节点
   */
  static createEndNode(options: {
    id?: string
    name?: string
    desc?: string
    output_type: 'text' | 'json'
    text_template?: string
    stream_switch?: boolean
    json_params?: JsonParam[]
  }): Node {
    const { id, name, desc, ...configParams } = options
    
    return {
      id: id || WorkflowTransform.generateNodeId('End'),
      name: name || '结束节点',
      desc: desc || '工作流的出口节点',
      type: 'End',
      config: NodeConfigFactory.createEndConfig(configParams)
    }
  }
}

export default NodeCreator
