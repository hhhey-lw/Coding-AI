package com.coding.agentflow.service.impl;

import cn.hutool.json.JSONUtil;
import com.coding.Application;
import com.coding.agentflow.model.model.AgentFlowConfig;
import com.coding.agentflow.service.AgentFlowService;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.state.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = Application.class)
public class AgentFlowServiceImplTest {

    @Autowired
    private AgentFlowService agentFlowService;

    @Test
    public void testConvertToCompiledGraph_ComplexFlow() throws GraphStateException {
        String jsonConfig = """
            {
              "id": "complex-flow",
              "name": "Complex Flow with Condition",
              "nodes": [
                {
                  "id": "START",
                  "type": "START",
                  "label": "Start",
                  "configParams": {}
                },
                {
                  "id": "llm",
                  "type": "LLM",
                  "label": "Llm",
                  "configParams": {
                    "model": "qwen-plus",
                    "prompt": "Hi",
                    "systemPrompt": "你是一个乐于助人的AI助手"
                  }
                },
                {
                  "id": "END",
                  "type": "END",
                  "label": "End",
                  "configParams": {}
                }
              ],
              "edges": [
                {
                  "source": "START",
                  "target": "llm"
                },
                {
                  "source": "llm",
                  "target": "END"
                }
              ]
            }
            """;

        AgentFlowConfig agentFlowConfig = JSONUtil.toBean(jsonConfig, AgentFlowConfig.class);
        
        System.out.println("Parsed Config Nodes: " + agentFlowConfig.getNodes().size());
        if (!agentFlowConfig.getNodes().isEmpty()) {
             System.out.println("Node 0 Type: " + agentFlowConfig.getNodes().get(0).getType());
        }

        CompiledGraph compiledGraph = agentFlowService.convertToCompiledGraph(agentFlowConfig);
        assertNotNull(compiledGraph, "Compiled graph should not be null");
        System.out.println("Graph compiled successfully.");

        Optional<OverAllState> allState = compiledGraph.invoke(Map.of("messages", "HI"));
        if (allState.isPresent()) {
            System.out.println(allState.get().data());
        }
    }
}
