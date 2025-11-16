package com.coding.workflow.service.runtime;

import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.ValueFromEnum;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.impl.processor.ScriptExecuteProcessor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class ScriptNodeTest {

    @Resource
    private ScriptExecuteProcessor scriptExecuteProcessor;

    @Test
    void testScriptExe() throws InterruptedException {
        Node node = new Node();
        node.setId("script_exe");
        node.setType("Script");
        node.setName("脚本执行");
        Node.InputParam inputParam = new Node.InputParam();
        inputParam.setKey("input");
        inputParam.setType("String");
        inputParam.setValue("${Start.output}");
        inputParam.setValueFrom(ValueFromEnum.refer.name());

        Node.OutputParam outputParam = new Node.OutputParam();
        outputParam.setKey("key0");
        outputParam.setType("string");

        Node.OutputParam outputParam2 = new Node.OutputParam();
        outputParam2.setKey("key1");
        outputParam2.setType("array");

        Node.OutputParam outputParam3 = new Node.OutputParam();
        outputParam3.setKey("key2");
        outputParam3.setType("object");

        node.setConfig(Node.NodeCustomConfig.of(List.of(
                inputParam
        ), List.of(
                outputParam, outputParam2, outputParam3
        ), Map.of(
                "scriptType", "javascript", // 模型名称
                "scriptContent",
                "function main(params) {\n" +
                    "    // 构建输出对象\n" +
                    "    console.log(params)\n" +
                    "    const ret = {\n" +
                    "        \"key0\": params.input,\n" +
                    "        \"key1\": [\"hello\", \"world\"], // 输出一个数组\n" +
                    "        \"key2\": { // 输出一个Object\n" +
                    "            \"key21\": \"hi\"\n" +
                    "        },\n" +
                    "    };\n" +
                    "\n" +
                    "    return ret;\n" +
                    "}"
        )));
        WorkflowContext context = new WorkflowContext();
        context.setConfigId(-1L);
        context.setInstanceId(-1L);
        context.getVariablesMap().put("Start", Map.of("output", "写一首关于春天的诗歌"));
        NodeResult nodeResult = scriptExecuteProcessor.innerExecute(null, node, context);
        System.out.println(JSONUtil.toJsonStr(nodeResult));


        node.setConfig(Node.NodeCustomConfig.of(List.of(
                inputParam
        ),  List.of(
                outputParam, outputParam2, outputParam3
        ), Map.of(
                "scriptType", "python", // 模型名称
                "scriptContent",
                "def main(params):\n" +
                        "    # 构建输出对象\n" +
                        "    print(params)\n" +
                        "    ret = {\n" +
                        "        'key0': params['input'],\n" +
                        "        'key1': ['hello', 'world'],  # 输出一个列表\n" +
                        "        'key2': {  # 输出一个字典\n" +
                        "            'key21': 'hi'\n" +
                        "        }\n" +
                        "    }\n" +
                        "    return ret"
        )));
        nodeResult = scriptExecuteProcessor.innerExecute(null, node, context);
        System.out.println(JSONUtil.toJsonStr(nodeResult));

    }

}
