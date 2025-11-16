package com.coding.core.service;

import com.coding.core.model.vo.MessageVO;
import reactor.core.publisher.Flux;

public interface AiAgentService {

    Flux<MessageVO> chat(String prompt);

}
