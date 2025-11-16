package com.coding.core.service.impl;

import com.coding.core.model.vo.MessageVO;
import com.coding.core.service.AiAgentService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiAgentServiceImpl implements AiAgentService {

    @Override
    public Flux<MessageVO> chat(String prompt) {
        return null;
    }



}
