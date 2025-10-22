package com.coding.graph.core.streaming;

import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.state.OverAllState;
import org.reactivestreams.FlowAdapters;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * 流式聊天生成器接口。
 */
public interface StreamingChatGenerator {

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private Function<ChatResponse, Map<String, Object>> mapResult;
        private String startingNode;
        private OverAllState startingState;

        public Builder mapResult(Function<ChatResponse, Map<String, Object>> mapResult) {
            this.mapResult = mapResult;
            return this;
        }

        public Builder startingNode(String node) {
            this.startingNode = node;
            return this;
        }

        public Builder startingState(OverAllState state) {
            this.startingState = state;
            return this;
        }

        public AsyncGenerator<? extends NodeOutput> build(Flux<ChatResponse> flux) {
            return buildInternal(flux,
                    chatResponse -> new StreamingOutput(startingNode, startingState, chatResponse));
        }

        /**
         * 构建并返回一个处理聊天响应的 AsyncGenerator 实例，
         * 将每个 ChatResponse 包装到一个 StreamingOutput 对象中。此方法允许
         * 下游消费者访问每个流式结果的完整 ChatResponse（而不仅仅是文本块），
         * 从而实现更丰富的输出处理，例如提取元数据、finishReason 或工具调用信息。
         * @param flux ChatResponse 对象的 Flux 流
         * @param outputMapper 将 ChatResponse 映射为 StreamingOutput 的函数
         * @return 一个生成包含完整 ChatResponse 的 StreamingOutput 实例的 AsyncGenerator
         */
        private AsyncGenerator<? extends NodeOutput> buildInternal(Flux<ChatResponse> flux,
                                                                   Function<ChatResponse, StreamingOutput> outputMapper) {
            Objects.requireNonNull(flux, "flux 不能为空");
            Objects.requireNonNull(mapResult, "mapResult 不能为空");

            var result = new AtomicReference<ChatResponse>(null);

            Consumer<ChatResponse> mergeMessage = (response) -> {
                result.updateAndGet(lastResponse -> {

                    if (lastResponse == null) {
                        return response;
                    }

                    final var currentMessage = response.getResult().getOutput();

                    if (currentMessage.hasToolCalls()) {
                        return response;
                    }

                    final var lastMessageText = requireNonNull(lastResponse.getResult().getOutput().getText(),
                            "lastResponse text cannot be null");

                    final var currentMessageText = currentMessage.getText();

                    var newMessage = new AssistantMessage(
                            currentMessageText != null ? lastMessageText.concat(currentMessageText) : lastMessageText,
                            currentMessage.getMetadata(), currentMessage.getToolCalls(), currentMessage.getMedia());

                    var newGeneration = new Generation(newMessage, response.getResult().getMetadata());
                    return new ChatResponse(List.of(newGeneration), response.getMetadata());

                });
            };

            var processedFlux = flux
                    .filter(response -> response.getResult() != null && response.getResult().getOutput() != null)
                    .doOnNext(mergeMessage)
                    .map(outputMapper);

            return FlowGenerator.fromPublisher(FlowAdapters.toFlowPublisher(processedFlux),
                    () -> mapResult.apply(result.get()));
        }
    }

}
