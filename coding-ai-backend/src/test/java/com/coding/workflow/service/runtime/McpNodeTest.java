package com.coding.workflow.service.runtime;

import cn.hutool.json.JSONUtil;
import com.coding.core.common.Result;
import com.coding.workflow.service.ai.McpExecuteService;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.enums.ValueFromEnum;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;
import com.coding.workflow.service.impl.processor.MCPExecuteProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author weilong
 * @date 2025/9/29
 */
@SpringBootTest
public class McpNodeTest {

    @Resource
    private McpExecuteService mcpExecuteService;

    @Resource
    private MCPExecuteProcessor mcpExecuteProcessor;

    @Test
    void test() {

        McpServerCallToolRequest request = new McpServerCallToolRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setServerCode("bocha_search");
        request.setToolName("bocha_web_search");
        request.setToolParams(Map.of(
                "query", "国庆出行去哪玩比较好",
                "count", "10"
        ));

        Result<McpServerCallToolResponse> mcpServerCallToolResponseResult = mcpExecuteService.callTool(request);
        System.out.println(JSONUtil.toJsonStr(mcpServerCallToolResponseResult));
    }

    @Test
    void testMcpNode() throws InterruptedException {
        Node node = new Node();
        node.setId("mcp");
        node.setDesc(NodeTypeEnum.MCP.getDesc());
        node.setName(NodeTypeEnum.MCP.getCode());
        node.setType(NodeTypeEnum.MCP.getCode());

        Node.NodeCustomConfig nodeCustomConfig = new Node.NodeCustomConfig();
        nodeCustomConfig.setInputParams(List.of(
//                Node.InputParam.of("location", "string", ValueFromEnum.input, "120.348609,30.312892"),
//                Node.InputParam.of("keywords", "string", ValueFromEnum.input, "川菜"),
//                Node.InputParam.of("radius", "string", ValueFromEnum.input, "3000")
                Node.InputParam.of("query", "string", ValueFromEnum.input, "国庆出行去哪玩比较好"),
                Node.InputParam.of("count", "string", ValueFromEnum.input, "10")
        ));
        nodeCustomConfig.setOutputParams(Collections.emptyList());
        nodeCustomConfig.setNodeParam(Map.of(
//            "server_code", "gaode_map",
//                "tool_name", "maps_around_search"
                "server_code", "bocha_search",
            "tool_name", "bocha_web_search"
        ));
        node.setConfig(nodeCustomConfig);

        WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setAppId(123L);
        workflowContext.setInstanceId(456L);
        NodeResult nodeResult = mcpExecuteProcessor.innerExecute(null, node, workflowContext);
        System.out.println(JSONUtil.toJsonStr(nodeResult.getOutput()));
    }

}
