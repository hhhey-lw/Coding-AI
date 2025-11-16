package com.coding.graph.core.node;

import com.coding.graph.core.state.OverAllState;
import lombok.Getter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

import static java.lang.String.format;

@Getter
public class StreamingOutput extends NodeOutput{
    // 完整响应
    private final ChatResponse chatResponse;

    public StreamingOutput(String node, OverAllState state, ChatResponse chatResponse) {
        super(node, state);
        this.chatResponse = chatResponse;
    }

    @Override
    public String toString() {
        AssistantMessage assistantMessage = getChatResponse().getResult().getOutput();
        if (getNode() == null) {
            return format("StreamingOutput{chunk=%s}", assistantMessage.hasToolCalls() ? assistantMessage.getText() : assistantMessage.getToolCalls());
        }
        return format("StreamingOutput{node=%s, response=%s, chunk=%s}", getNode(), getChatResponse().getResult().getOutput(), assistantMessage.getText());
    }

}
