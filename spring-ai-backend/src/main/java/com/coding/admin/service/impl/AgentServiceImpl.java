package com.coding.admin.service.impl;

import com.coding.admin.model.vo.MessageVO;
import com.coding.admin.service.AgentService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AgentServiceImpl implements AgentService {

    @Override
    public Flux<MessageVO> chat(String prompt) {
        return null;
    }



}
