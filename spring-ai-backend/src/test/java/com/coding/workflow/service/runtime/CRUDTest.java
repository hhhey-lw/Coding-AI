package com.coding.workflow.service.runtime;

import com.coding.admin.model.model.WorkflowNodeInstanceModel;
import com.coding.admin.repository.WorkflowNodeInstanceRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CRUDTest {

    @Resource
    private WorkflowNodeInstanceRepository workflowNodeInstanceRepository;

    @Test
    public void test() {
        int i = workflowNodeInstanceRepository.add(WorkflowNodeInstanceModel.builder()
                .nodeName("test")
                .build());

        System.out.println(i);
    }

}
