package com.coding.workflow.service.runtime;

import com.coding.admin.model.entity.WorkflowConfigDO;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.WorkflowConfig;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.WorkflowCoreEngine;
import com.coding.workflow.service.impl.processor.EndExecuteProcessor;
import com.coding.workflow.service.impl.processor.JudgeExecuteProcessor;
import com.coding.workflow.service.impl.processor.StartExecuteProcessor;
import com.coding.workflow.service.impl.processor.TextGenExecuteProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
public class WorkflowCoreEngineTest {

    @Resource
    private StartExecuteProcessor startExecuteProcessor;
    @Resource
    private TextGenExecuteProcessor textGenExecuteProcessor;
    @Resource
    private JudgeExecuteProcessor judgeExecuteProcessor;
    @Resource
    private EndExecuteProcessor endExecuteProcessor;
    @Resource
    private WorkflowCoreEngine workflowCoreEngine;

    @Test
    public void testSimpleWorkflowExecution() throws InterruptedException {
        // 构造节点
        Node start = new Node();
        start.setId("Start");
        start.setType("start");

        Node llm = new Node();
        llm.setId("LLM");
        llm.setType("llm");
        llm.setName("LLM");

        Node.InputParam llmInputParam = new Node.InputParam();
        llmInputParam.setValueFrom("refer");
        llmInputParam.setKey("input");
        llmInputParam.setRequired(true);
        llmInputParam.setType("string");
        llmInputParam.setValue("${Start.output}");
        llm.setConfig(Node.NodeCustomConfig.of(List.of(llmInputParam), null, Map.of(
            "sys_prompt_content", "你是一个乐于助人的${Start.input}",
            "prompt_content", "哈哈哈，你好呀",
            "model_config", Map.of(
                "model_id", "deepseek-v3",
                "model_name", "deepseek-v3",
                "params", List.of(
                        Map.of(
                            "key", "temperature",
                            "value", 0.8,
                            "enable", true
                        )
                    )
                )
        )));

        Node judge = new Node();
        judge.setId("Judge");
        judge.setType("judge");
        Node.NodeCustomConfig customConfig = Node.NodeCustomConfig.builder()
                .nodeParam(Map.of(
                        "branches", List.of(
                                Map.of(
                                        "id", "End",
                                        "label", "",
                                        "logic", "AND",
                                        "conditions", List.of(
                                                Map.of("leftKey", Map.of(
                                                                "key", "",
                                                                "type", "string",
                                                                "value", "${LLM.output}"
                                                        ),
                                                        "operator", "isNotNull",
                                                        "rightValue", "")
                                        )
                                ),
                                Map.of(
                                        "id", "End2",
                                        "label", "",
                                        "logic", "AND",
                                        "conditions", List.of(
                                                Map.of("leftKey", Map.of(
                                                                "key", "",
                                                                "type", "string",
                                                                "value", "${LLM.output}"
                                                        ),
                                                        "operator", "isNull",
                                                        "rightValue", "")
                                        )
                                )
                        )
                ))
                .build();
        judge.setConfig(customConfig);

        Node end = new Node();
        end.setId("End");
        end.setType("end");
        Node.NodeCustomConfig endConfig = Node.NodeCustomConfig.builder()
                .nodeParam(Map.of(
                        "output_type", "json",
                        "text_template", "工作流执行结束，输出内容：${input}",
                        "stream_switch", false,
                        "json_params", List.of(
                                Map.of(
                                        "key", "output",
                                        "value", "${LLM.output}",
                                        "value_from", "refer",
                                        "type", "string"
                                )
                        )
                ))
                        .build();
        end.setConfig(endConfig);

        Node end2 = new Node();
        end2.setId("End2");
        end2.setType("end");


        // 构造边
        Edge edge1 = new Edge();
        edge1.setSource("Start");
        edge1.setTarget("LLM");

        Edge edge2 = new Edge();
        edge2.setSource("LLM");
        edge2.setTarget("Judge");

        Edge edge3 = new Edge();
        edge3.setSource("Judge");
        edge3.setTarget("End");

        Edge edge4 = new Edge();
        edge4.setSource("Judge");
        edge4.setTarget("End2");

        // 构造工作流配置
        WorkflowConfig config = new WorkflowConfig();
        config.setNodes(Arrays.asList(start, llm, judge, end, end2));
        config.setEdges(Arrays.asList(edge1, edge2, edge3, edge4));

        // 构造引擎 - 使用Spring管理的组件
        // 注册处理器
        workflowCoreEngine.registerProcessor("start", startExecuteProcessor);
        workflowCoreEngine.registerProcessor("end", endExecuteProcessor);
        workflowCoreEngine.registerProcessor("llm", textGenExecuteProcessor);
        workflowCoreEngine.registerProcessor("judge", judgeExecuteProcessor);

        // 执行工作流
        WorkflowContext context = new WorkflowContext();
        Long taskId = workflowCoreEngine.executeWorkflow(config, context);
        // 等待异步执行完成
        System.out.println("工作流执行任务ID: " + taskId);
        Thread.sleep(30000);

    }

    @Test
    public void testSimpleWorkflowExecutionWithDO() throws InterruptedException {
        // 构造节点
        Node start = new Node();
        start.setId("Start");
        start.setType("start");
        start.setConfig(Node.NodeCustomConfig.of(null, null, Map.of(
                "input", "故事家，擅长为儿童编写有趣的故事"
        )));

        Node llm = new Node();
        llm.setId("LLM");
        llm.setType("llm");
        llm.setName("LLM");

        Node.InputParam llmInputParam = new Node.InputParam();
        llmInputParam.setValueFrom("refer");
        llmInputParam.setKey("input");
        llmInputParam.setRequired(true);
        llmInputParam.setType("string");
        llmInputParam.setValue("${Start.output}");
        llm.setConfig(Node.NodeCustomConfig.of(List.of(llmInputParam), null, Map.of(
                "sys_prompt_content", "你是一个乐于助人的${Start.input}",
                "prompt_content", "哈哈哈，你好呀",
                "model_config", Map.of(
                        "provider", "BaiLian",
                        "model_id", "deepseek-v3",
                        "model_name", "deepseek-v3",
                        "params", List.of(
                                Map.of(
                                        "key", "temperature",
                                        "value", 0.8,
                                        "enable", true
                                )
                        )
                )
        )));

        Node judge = new Node();
        judge.setId("Judge");
        judge.setType("judge");
        Node.NodeCustomConfig customConfig = Node.NodeCustomConfig.builder()
                .nodeParam(Map.of(
                        "branches", List.of(
                                Map.of(
                                        "id", "End",
                                        "label", "",
                                        "logic", "AND",
                                        "conditions", List.of(
                                                Map.of("left_key", Map.of(
                                                                "key", "",
                                                                "type", "string",
                                                                "value", "${LLM.output}"
                                                        ),
                                                        "operator", "isNotNull",
                                                        "right_value", "")
                                        )
                                ),
                                Map.of(
                                        "id", "End2",
                                        "label", "",
                                        "logic", "AND",
                                        "conditions", List.of(
                                                Map.of("left_key", Map.of(
                                                                "key", "",
                                                                "type", "string",
                                                                "value", "${LLM.output}"
                                                        ),
                                                        "operator", "isNull",
                                                        "right_value", "")
                                        )
                                )
                        )
                ))
                .build();
        judge.setConfig(customConfig);

        Node end = new Node();
        end.setId("End");
        end.setType("end");
        Node.NodeCustomConfig endConfig = Node.NodeCustomConfig.builder()
                .nodeParam(Map.of(
                        "output_type", "json",
                        "text_template", "工作流执行结束，输出内容：${input}",
                        "stream_switch", false,
                        "json_params", List.of(
                                Map.of(
                                        "key", "output",
                                        "value", "${LLM.output}",
                                        "value_from", "refer",
                                        "type", "string"
                                )
                        )
                ))
                .build();
        end.setConfig(endConfig);

        Node end2 = new Node();
        end2.setId("End2");
        end2.setType("end");

        // 构造边
        Edge edge1 = new Edge();
        edge1.setSource("Start");
        edge1.setTarget("LLM");

        Edge edge2 = new Edge();
        edge2.setSource("LLM");
        edge2.setTarget("Judge");

        Edge edge3 = new Edge();
        edge3.setSource("Judge");
        edge3.setTarget("End");

        Edge edge4 = new Edge();
        edge4.setSource("Judge");
        edge4.setTarget("End2");

        // 构造WorkflowConfigDO对象
        WorkflowConfigDO configDO = new WorkflowConfigDO();
        configDO.setId(1L);
        configDO.setName("测试工作流");
        configDO.setDescription("简单的工作流测试");
        configDO.setAppId(1L);
        configDO.setVersion("1.0.0");
        configDO.setCreator(1L);
        configDO.setCreateTime(LocalDateTime.now());
        configDO.setUpdateTime(LocalDateTime.now());
        configDO.setStatus(1);

        // 将nodes和edges转换为JSON字符串
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            configDO.setNodes(objectMapper.writeValueAsString(Arrays.asList(start, llm, judge, end, end2)));
            configDO.setEdges(objectMapper.writeValueAsString(Arrays.asList(edge1, edge2, edge3, edge4)));
        } catch (Exception e) {
            throw new RuntimeException("序列化节点和边信息失败", e);
        }

        // 从DO对象构建WorkflowConfig用于执行
        WorkflowConfig config = new WorkflowConfig();
        try {
            List<Node> nodeList = objectMapper.readValue(configDO.getNodes(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Node.class));
            List<Edge> edgeList = objectMapper.readValue(configDO.getEdges(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Edge.class));
            config.setNodes(nodeList);
            config.setEdges(edgeList);
        } catch (Exception e) {
            throw new RuntimeException("反序列化节点和边信息失败", e);
        }

        // 注册处理器
        workflowCoreEngine.registerProcessor("start", startExecuteProcessor);
        workflowCoreEngine.registerProcessor("end", endExecuteProcessor);
        workflowCoreEngine.registerProcessor("llm", textGenExecuteProcessor);
        workflowCoreEngine.registerProcessor("judge", judgeExecuteProcessor);

        // 执行工作流
        WorkflowContext context = new WorkflowContext();
        context.getUserMap().put("input", "小助手呀");

        Long taskId = workflowCoreEngine.executeWorkflow(config, context);

        System.out.println("工作流配置DO: " + configDO);
        System.out.println("工作流执行任务ID: " + taskId);
        Thread.sleep(30000);
    }



}
