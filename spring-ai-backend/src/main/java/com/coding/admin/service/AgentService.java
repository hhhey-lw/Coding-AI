package com.coding.admin.service;

import com.coding.admin.common.MessageVO;
import reactor.core.publisher.Flux;

public interface AgentService {

    Flux<MessageVO> chat(String prompt);

}
