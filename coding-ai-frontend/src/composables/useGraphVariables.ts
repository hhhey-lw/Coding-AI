import { computed, unref, type Ref } from 'vue'
import { useVueFlow } from '@vue-flow/core'

export interface VariableOption {
  label: string
  value: string
  type: string // 'string' | 'list' | 'state'
  nodeId: string
  nodeType: string
}

export function useGraphVariables(currentNodeId: string | Ref<string>) {
  const { edges, findNode } = useVueFlow()

  const predecessors = computed(() => {
    const id = unref(currentNodeId)
    if (!id) return []

    const visited = new Set<string>()
    const queue = [id]
    const predecessorNodes = []

    // Map source -> targets to find incoming edges efficiently?
    // Actually edges is an array. Building an adjacency list (target -> sources) is better.
    const targetToSources = new Map<string, string[]>()
    edges.value.forEach(edge => {
      if (!targetToSources.has(edge.target)) {
        targetToSources.set(edge.target, [])
      }
      targetToSources.get(edge.target)?.push(edge.source)
    })

    // BFS backwards
    let head = 0
    while(head < queue.length) {
        const curr = queue[head++]
        const sources = targetToSources.get(curr) || []
        
        for (const sourceId of sources) {
            if (!visited.has(sourceId)) {
                visited.add(sourceId)
                queue.push(sourceId)
                const node = findNode(sourceId)
                if (node) {
                    predecessorNodes.push(node)
                }
            }
        }
    }
    
    return predecessorNodes
  })

  const availableVariables = computed<VariableOption[]>(() => {
    const vars: VariableOption[] = []

    predecessors.value.forEach(node => {
      // AgentFlow 中所有节点的 node.type 都是 'custom'，实际类型存储在 node.data.type
      const type = node.data?.type || node.type
      const id = node.id
      const label = node.label || node.data?.label || id

      // 1. Start Node - 使用命名空间格式 {{nodeId.key}}
      if (type === 'start' || id.startsWith('start')) {
         const flowState = node.data?.flowState || []
         const hasMessages = flowState.some((s: any) => s.key === 'messages')
         
         flowState.forEach((state: {key: string, value: string}) => {
             if (state.key) {
                 vars.push({
                     label: `Start: ${state.key}`,
                     value: `{{${id}.${state.key}}}`,
                     type: 'state',
                     nodeId: id,
                     nodeType: type
                 })
             }
         })

         if (!hasMessages) {
             vars.push({
                 label: `Start: messages`,
                 value: `{{${id}.messages}}`,
                 type: 'state',
                 nodeId: id,
                 nodeType: type
             })
         }
      }
      // 2. LLM / Agent Node - 使用命名空间格式 {{nodeId.output}}
      else if (type === 'llm' || type === 'agent' || id.startsWith('llm') || id.startsWith('agent')) {
          vars.push({
              label: `${label} (Output)`,
              value: `{{${id}.output}}`,
              type: 'string',
              nodeId: id,
              nodeType: type
          })
      }
      // 3. Direct Reply / End Node
      else if (type === 'reply' || type === 'end') {
           vars.push({
              label: `${label} (Output)`,
              value: `{{${id}.finalResult}}`,
              type: 'string',
              nodeId: id,
              nodeType: type
          })
      }
      // 4. Human Input
      else if (type === 'human') {
           vars.push({
              label: `${label} (Input)`,
              value: `{{${id}.humanInput}}`,
              type: 'string',
              nodeId: id,
              nodeType: type
          })
      }
      // 5. Retriever
      else if (type === 'retriever') {
           vars.push({
              label: `${label} (Documents)`,
              value: `{{${id}.documents}}`,
              type: 'list',
              nodeId: id,
              nodeType: type
          })
      }
      // 6. Tool
      else if (type === 'tool') {
           vars.push({
              label: `${label} (Result)`,
              value: `{{${id}.toolResult}}`,
              type: 'string',
              nodeId: id,
              nodeType: type
          })
      }
      // Condition nodes ignored as per requirement
    })

    return vars
  })

  return {
    availableVariables
  }
}
