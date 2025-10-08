package com.coding.graph.core.node;

import com.coding.graph.core.state.OverAllState;
import lombok.Getter;
import org.springframework.ai.chat.model.ChatResponse;

import static java.lang.String.format;

@Getter
public class StreamingOutput extends NodeOutput{
    // 响应的Chunk块
    private final String chunk;
    // 完整响应
    private final ChatResponse chatResponse;

    public StreamingOutput(String node, OverAllState state, ChatResponse chatResponse) {
        super(node, state);
        this.chatResponse = chatResponse;
        this.chunk = null;
    }

    public StreamingOutput(String chunk, String node, OverAllState state) {
        super(node, state);
        this.chunk = chunk;
        this.chatResponse = null;
    }

    @Override
    public String toString() {
        if (getNode() == null) {
            return format("StreamingOutput{chunk=%s}", getChunk());
        }
        return format("StreamingOutput{node=%s, state=%s, chunk=%s}", getNode(), getState(), getChunk());
    }

}
