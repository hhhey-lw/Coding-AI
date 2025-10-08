package com.coding.workflow.service.runtime;

import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.impl.processor.SendEmailExecuteProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

@SpringBootTest
public class SendEmailNodeTest {

    @Resource
    private SendEmailExecuteProcessor sendEmailExecuteProcessor;


    @Test
    void sendEmail() throws InterruptedException {

        Node node = new Node();
        node.setType(NodeTypeEnum.EMAIL.getCode());
        node.setId("321-email");
        node.setName("发送邮件节点");

        Node.NodeCustomConfig customConfig = new Node.NodeCustomConfig();
        customConfig.setInputParams(Collections.emptyList());
        customConfig.setOutputParams(Collections.emptyList());
        customConfig.setNodeParam(Map.of(
                "to", "longwei0157@163.com",
                "from", "1410124534@qq.com",
                "subject", "测试邮件",
                "content", "<h1>这是一封测试邮件</h1>",
                "html", true,
                "authorization", "avmvaqqeifelhcjb"
        ));
        node.setConfig(customConfig);


        WorkflowContext context = new WorkflowContext();
        context.setInstanceId(321L);
        context.setAppId(123L);
        sendEmailExecuteProcessor.innerExecute(null, node, context);
    }

}
