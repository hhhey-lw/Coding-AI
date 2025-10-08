# Spring AI Front - 工作流可视化编辑器

一个基于 Vue 3 + TypeScript + Vite + Vue Flow 的现代化工作流设计器前端项目。

## 🚀 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 的超集，提供类型安全
- **Vite** - 下一代前端构建工具
- **Element Plus** - 企业级 UI 组件库
- **Vue Flow** - 专业的流程图编辑器
- **Vue Router** - 路由管理

## ✨ 功能特性

- ⚡ **拖拽式节点编辑** - 从节点面板拖拽创建节点
- 🔗 **可视化连线** - 拖拽节点连接点创建工作流
- ⚙️ **实时配置** - 点击节点弹出侧边栏编辑详细配置
- 🎨 **丰富节点类型** - 支持 LLM、图像生成、视频生成、脚本执行等 11 种节点
- 📡 **前后端数据转换** - 自动转换前端展示格式和后端存储格式
- ✅ **数据验证** - 内置完整的工作流验证机制
- 🌙 **主题切换** - 支持明暗主题
- 📱 **响应式设计** - 适配不同屏幕尺寸

## 📁 项目结构

```
spring-ai-front/
├── config/                     # 配置文件
│   └── vite.config.ts         # Vite 构建配置
├── public/                     # 静态资源
│   └── vite.svg               # 图标
├── src/                        # 源代码
│   ├── api/                    # API 接口
│   │   └── workflow.ts        # 工作流 API
│   ├── components/             # 组件
│   │   ├── Index.vue          # 首页组件
│   │   └── Workflow/          # 工作流相关组件
│   │       ├── WorkflowDesigner.vue    # 主设计器（完整布局）
│   │       ├── WorkflowEditor.vue      # 画布编辑器（Vue Flow集成）
│   │       ├── NodePalette.vue         # 左侧节点面板
│   │       ├── NodeSidebar.vue         # 右侧节点详情侧边栏
│   │       ├── CustomNode.vue          # 自定义节点组件 ⭐样式在这里
│   │       └── DropzoneBackground.vue  # 背景网格组件
│   ├── examples/               # 使用示例
│   │   └── nodeUsageExample.ts # 节点使用示例代码
│   ├── router/                 # 路由配置
│   │   └── index.ts           # 路由定义
│   ├── types/                  # TypeScript 类型定义
│   │   ├── workflow.ts        # 工作流核心类型
│   │   └── nodeConfigs.ts     # 节点配置接口 ⭐
│   ├── utils/                  # 工具函数
│   │   ├── workflowTransform.ts # 前后端数据转换
│   │   └── nodeCreator.ts      # 节点创建工厂
│   ├── App.vue                # 根组件
│   ├── main.ts                # 应用入口
│   ├── style.css              # 全局样式
│   └── vite-env.d.ts          # Vite 类型声明
├── index.html                  # HTML 入口
├── package.json               # 项目配置
├── tsconfig.json              # TypeScript 配置
└── README.md                  # 项目文档
```

## 🛠️ 开发

### 环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器将在 `http://localhost:3000` 启动。

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

### 预览生产构建

```bash
npm run preview
```

## 🎯 核心功能说明

### 1. 工作流设计器

访问 `/workflow` 路由进入工作流设计器，包含三个主要区域：

- **左侧节点面板**：分类显示所有可用节点，支持搜索和拖拽
- **中间画布区域**：拖拽创建节点和连线，缩放平移等操作
- **右侧详情面板**：点击节点后弹出，编辑节点配置

### 2. 支持的节点类型

#### 基础节点
- **Start** - 开始节点：工作流入口
- **End** - 结束节点：工作流出口，支持 JSON/文本输出
- **Output** - 输出节点：数据格式化输出

#### AI 处理节点
- **LLM** - 大语言模型节点：GPT-4 等模型处理
- **ImgGen** - 图像生成节点：AI 图像生成
- **VideoGen** - 视频生成节点：AI 视频生成
- **MusicGen** - 音乐生成节点：AI 音乐生成

#### 工具节点
- **MCP** - MCP 协议节点：调用 MCP 服务
- **Script** - 脚本执行节点：JavaScript/Python 脚本
- **Email** - 邮件发送节点：发送邮件
- **Judge** - 条件判断节点：逻辑分支

## 📋 节点配置说明

### 节点数据结构

每个节点包含以下核心配置：

```typescript
interface Node {
  id: string                  // 唯一标识符
  name: string               // 节点名称
  desc?: string              // 节点描述
  type: string               // 节点类型（Start, LLM, End等）
  config: NodeCustomConfig   // 节点配置
}

interface NodeCustomConfig {
  input_params?: InputParam[]   // 输入参数配置
  output_params?: OutputParam[] // 输出参数配置
  node_param?: Record<string, any> // 节点特定配置
}
```

### LLM 节点配置示例

```typescript
{
  name: 'GPT-4处理',
  type: 'LLM',
  config: {
    input_params: [
      { key: 'input', type: 'string', value_from: 'refer', required: true }
    ],
    output_params: [
      { key: 'output', type: 'string', desc: 'LLM处理结果' },
      { key: 'usage', type: 'object', desc: 'Token使用情况' }
    ],
    node_param: {
      provider: 'openai',
      model: 'gpt-4',
      input: '请输入提示词...',
      temperature: 0.7,
      max_tokens: 1000
    }
  }
}
```

### 图像生成节点配置示例

```typescript
{
  name: '图像生成',
  type: 'ImgGen',
  config: {
    node_param: {
      provider: 'volcengine',
      modelId: 'doubao-seedream-4-0-250828',
      input: '生成一张美丽的风景图',
      imgSize: '4k',
      maxImages: 2,
      watermark: false
    }
  }
}
```

### End 节点配置示例

```typescript
{
  name: '结束节点',
  type: 'End',
  config: {
    input_params: [
      { key: 'input', type: 'String', value_from: 'refer', required: true }
    ],
    node_param: {
      output_type: 'json',
      json_params: [
        {
          key: 'result',
          value: '${LLM.output}',
          value_from: 'refer',
          type: 'string'
        }
      ]
    }
  }
}
```

## 🎨 节点样式自定义

### 节点样式文件位置

节点的样式定义在 `src/components/Workflow/CustomNode.vue` 文件中。

### 主要样式类

```css
.custom-node          /* 节点容器 */
.node-header          /* 节点头部 */
.node-section         /* 节点内容区块 */
.section-title        /* 区块标题 */
.param-item           /* 参数项 */
.config-item          /* 配置项 */
.node-handle          /* 连接点 */
```

### 节点类型样式

每种节点类型都有独特的头部渐变色：

```css
.node-start .node-header     /* 绿色渐变 */
.node-end .node-header       /* 红色渐变 */
.node-llm .node-header       /* 蓝色渐变 */
.node-imggen .node-header    /* 橙色渐变 */
```

### 自定义节点样式

在 `CustomNode.vue` 的 `<style scoped>` 部分修改样式：

```vue
<style scoped>
.custom-node {
  min-width: 200px;
  max-width: 280px;
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  /* 在这里修改节点的基础样式 */
}

.node-header {
  padding: 8px 12px;
  background: linear-gradient(135deg, #909399, #b1b3b8);
  /* 修改节点头部样式 */
}

/* 更多样式... */
</style>
```

### 连接点样式

连接点样式定义在 `CustomNode.vue` 底部：

```css
:deep(.vue-flow__handle) {
  width: 10px;
  height: 10px;
  border: 2px solid white;
  background: #555;
  /* 修改连接点样式 */
}
```

## 🔄 数据流转换

### 前后端数据转换

项目提供了完整的前后端数据转换工具：

```typescript
import { WorkflowTransform } from '@/utils/workflowTransform'

// 前端格式 → 后端格式
const backendRequest = WorkflowTransform.toBackendRequest(frontendConfig)

// 后端格式 → 前端格式
const frontendConfig = WorkflowTransform.toFrontendConfig(backendData)

// 验证工作流
const validation = WorkflowTransform.validateWorkflowConfig(config)
```

### 导出工作流数据

在设计器中点击 **"导出数据"** 按钮，控制台会显示：
- 前端格式的完整数据
- 后端格式的请求数据（可直接发送API）
- 数据验证结果
- 节点和连线详情
- Canvas 画布数据

## 📝 节点创建示例

### 使用 NodeCreator 创建节点

```typescript
import NodeCreator from '@/utils/nodeCreator'

// 创建 LLM 节点
const llmNode = NodeCreator.createLLMNode({
  name: 'GPT-4处理',
  provider: 'openai',
  model: 'gpt-4',
  input: '请处理这段文本：${Start.output}'
})

// 创建图像生成节点
const imageNode = NodeCreator.createImageGenNode({
  name: 'DALL-E生成',
  provider: 'volcengine',
  modelId: 'doubao-seedream-4-0-250828',
  input: '生成一张${LLM.output}的图片',
  imgSize: '4k'
})
```

### 完整工作流示例

```typescript
import { createImageGenWorkflow } from '@/examples/nodeUsageExample'

// 创建一个包含 Start → ImgGen → End 的完整工作流
const workflow = createImageGenWorkflow()

// 转换为后端格式
const request = WorkflowTransform.toBackendRequest(workflow)

// 发送给后端
await WorkflowAPI.createWorkflow(request)
```

## 🎓 使用指南

### 创建工作流

1. 访问 `http://localhost:3000/workflow`
2. 从左侧节点面板拖拽节点到画布
3. 拖拽节点的连接点创建连线
4. 点击节点打开右侧配置面板
5. 配置节点参数和连接关系
6. 点击"导出数据"查看生成的数据结构
7. 点击"保存"保存工作流

### 节点连接规则

- 每个节点有左右两个连接点
- **左侧（绿色）**：输入连接点
- **右侧（橙色）**：输出连接点
- 只能从输出连接点拖拽到输入连接点

### 参数引用

在节点配置中，可以引用前驱节点的输出：

```
语法：${NodeId.outputKey}
示例：${LLM.output}、${Start.output}
```

**重要**：只能引用**已连线的前驱节点**的输出参数。系统会递归查找所有上游节点。

## 📚 核心文件说明

### 类型定义文件

| 文件 | 说明 |
|------|------|
| `src/types/workflow.ts` | 工作流核心类型、节点/边接口、枚举定义 |
| `src/types/nodeConfigs.ts` | 每种节点类型的具体配置接口 ⭐ |

### 工具类文件

| 文件 | 说明 |
|------|------|
| `src/utils/nodeCreator.ts` | 节点创建工厂类 |
| `src/utils/workflowTransform.ts` | 前后端数据转换工具 |

### 组件文件

| 文件 | 说明 |
|------|------|
| `src/components/Workflow/WorkflowDesigner.vue` | 主设计器布局 |
| `src/components/Workflow/WorkflowEditor.vue` | Vue Flow 画布集成 |
| `src/components/Workflow/CustomNode.vue` | 自定义节点样式 ⭐ |
| `src/components/Workflow/NodePalette.vue` | 节点面板 |
| `src/components/Workflow/NodeSidebar.vue` | 配置侧边栏 |

## 🎨 自定义节点样式

### 修改节点外观

编辑 `src/components/Workflow/CustomNode.vue`：

```vue
<style scoped>
/* 1. 修改节点整体样式 */
.custom-node {
  min-width: 200px;      /* 最小宽度 */
  max-width: 280px;      /* 最大宽度 */
  background: white;     /* 背景色 */
  border-radius: 8px;    /* 圆角 */
}

/* 2. 修改节点头部样式 */
.node-header {
  padding: 8px 12px;
  background: linear-gradient(135deg, #909399, #b1b3b8);
  color: white;
}

/* 3. 为不同节点类型设置不同颜色 */
.node-llm .node-header {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.node-imggen .node-header {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
}

/* 4. 修改连接点样式 */
:deep(.vue-flow__handle) {
  width: 10px;
  height: 10px;
  background: #555;
}
</style>
```

### 节点布局结构

```
┌─────────────────────────┐
│ [图标] 节点名称      [×]  │ ← .node-header
├─────────────────────────┤
│ 输入                    │ ← .node-section
│  • param1 (string)      │ ← .param-item
├─────────────────────────┤
│ 配置                    │
│  • provider             │ ← .config-item
│  • model                │
├─────────────────────────┤
│ 输出                    │
│  • output (string)      │
└─────────────────────────┘
```

## 🔧 节点配置接口

所有节点配置接口定义在 `src/types/nodeConfigs.ts`。

### 添加新节点类型

**步骤 1**: 在 `nodeConfigs.ts` 添加接口

```typescript
export interface MyCustomNodeConfig extends NodeCustomConfig {
  node_param: {
    myField: string        // 自定义字段
    myOption?: number      // 可选字段
  }
}
```

**步骤 2**: 添加工厂方法

```typescript
export class NodeConfigFactory {
  static createMyCustomConfig(config: {
    myField: string
    myOption?: number
  }): MyCustomNodeConfig {
    return {
      input_params: [...],
      output_params: [...],
      node_param: config
    }
  }
}
```

**步骤 3**: 在 `nodeCreator.ts` 添加创建方法

```typescript
export class NodeCreator {
  static createMyCustomNode(options: {...}): Node {
    return {
      id: options.id || WorkflowTransform.generateNodeId('MyCustom'),
      name: options.name || '自定义节点',
      type: 'MyCustom',
      config: NodeConfigFactory.createMyCustomConfig(options)
    }
  }
}
```

**步骤 4**: 在 `WorkflowEditor.vue` 添加创建逻辑

```typescript
case 'MyCustom':
  node = NodeCreator.createMyCustomNode({
    name: nodeData?.name || '自定义节点',
    myField: 'default value'
  })
  break
```

**步骤 5**: 在 `NodePalette.vue` 添加到节点库

```typescript
{
  type: 'MyCustom',
  name: '自定义节点',
  description: '我的自定义节点',
  icon: 'Star',
  category: '自定义分类'
}
```

## 📡 API 接口

API 接口定义在 `src/api/workflow.ts`：

```typescript
import WorkflowAPI from '@/api/workflow'

// 创建工作流
const response = await WorkflowAPI.createWorkflow(request)

// 获取工作流
const workflow = await WorkflowAPI.getWorkflow(id)

// 更新工作流
await WorkflowAPI.updateWorkflow(id, request)

// 删除工作流
await WorkflowAPI.deleteWorkflow(id)

// 执行工作流
const execution = await WorkflowAPI.executeWorkflow(id, inputs)
```

## 🐛 调试技巧

### 查看工作流数据

点击工具栏的 **"导出数据"** 按钮，控制台会显示：

```
================================================================================
📊 WORKFLOW DATA EXPORT
================================================================================

【1. 前端格式数据 (WorkflowConfig)】
{
  "name": "新建工作流",
  "nodes": [...],
  "edges": [...]
}

【2. 后端格式数据 (WorkflowConfigAddRequest)】
{
  "name": "新建工作流",
  "version": "1.0.0",
  "nodes": [...],
  "edges": [...],
  "canvas": "{...}"  ← 可直接发送给后端的格式
}

【3. 数据验证结果】
验证通过: true

【4. 节点详情】
节点 1: 开始 (Start)
  - ID: Start_xxx
  - 位置: {x: 100, y: 100}
  - 输入参数: []
  - 输出参数: [{key: 'output', type: 'String'}]
  - 节点配置: {}

...
```

### 常见问题

**Q: 节点拖拽后不显示？**
- 检查控制台是否有错误
- 确认 NodeConfigFactory 正确导入
- 查看"节点已创建"日志

**Q: 节点详情配置为空？**
- 确认节点的 `config.node_param` 字段有值
- 检查 NodeCreator 是否正确配置

**Q: 输入参数下拉框没有选项？**
- 确认节点已连线
- 查看控制台 "Computing availableReferences" 日志
- 前驱节点必须有 output_params 配置

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

MIT License

---

**Happy Coding! 🎉**