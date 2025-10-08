package com.coding.graph.core.streaming;

import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.state.OverAllState;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

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
            return buildInternal(flux, chatResponse -> new StreamingOutput(chatResponse.getResult().getOutput().getText(), startingNode, startingState));
        }

        /**
         * 构建并返回一个处理聊天响应的 AsyncGenerator 实例，
         * 将每个 ChatResponse 包装到一个 StreamingOutput 对象中。此方法允许
         * 下游消费者访问每个流式结果的完整 ChatResponse（而不仅仅是文本块），
         * 从而实现更丰富的输出处理，例如提取元数据、finishReason 或工具调用信息。
         * @param flux ChatResponse 对象的 Flux 流
         * @return 一个生成包含完整 ChatResponse 的 StreamingOutput 实例的 AsyncGenerator
         */
        private AsyncGenerator<? extends NodeOutput> buildInternal(Flux<ChatResponse> flux,
                                                                   Function<ChatResponse, StreamingOutput> outputMapper) {
            Objects.requireNonNull(flux, "flux 不能为空");
            Objects.requireNonNull(mapResult, "mapResult 不能为空");

            // 1. 使用 BlockingQueue 作为流和生成器之间的桥梁
            final BlockingQueue<AsyncGenerator.Data<? extends NodeOutput>> queue = new LinkedBlockingQueue<>();

            // 2. 使用 publish().refCount(2) 将流分享给两个订阅者
            Flux<ChatResponse> sharedFlux = flux
                    .filter(response -> response.getResult() != null && response.getResult().getOutput() != null)
                    .publish()
                    .refCount(2);

            // 订阅者 1: 流式发送部分结果
            sharedFlux.map(outputMapper)
                    .map(AsyncGenerator.Data::of)
                    .cast(AsyncGenerator.Data.class)
                    .doOnError(error -> queue.add(AsyncGenerator.Data.error(error)))
                    .subscribe(queue::add);

            // 订阅者 2: 聚合最终结果
            sharedFlux.collectList().subscribe(responses -> {
                if (responses.isEmpty()) {
                    queue.add(AsyncGenerator.Data.done(mapResult.apply(null)));
                    return;
                }

                // 使用 reduce 操作合并所有响应块
                ChatResponse finalResponse = responses.stream().reduce(null, (acc, response) -> {
                    if (acc == null) {
                        return response;
                    }
                    final var currentMessage = response.getResult().getOutput();
                    if (currentMessage.hasToolCalls()) {
                        return response;
                    }
                    final var lastMessageText = requireNonNull(acc.getResult().getOutput().getText(),
                            "lastResponse text 不能为空");
                    final var currentMessageText = currentMessage.getText();
                    var newMessage = new AssistantMessage(
                            currentMessageText != null ? lastMessageText.concat(currentMessageText) : lastMessageText,
                            currentMessage.getMetadata(), currentMessage.getToolCalls(), currentMessage.getMedia());
                    var newGeneration = new Generation(newMessage, response.getResult().getMetadata());
                    return new ChatResponse(List.of(newGeneration), response.getMetadata());
                });

                // 发送带有最终聚合结果的完成信号
                queue.add(AsyncGenerator.Data.done(mapResult.apply(finalResponse)));
            });

            // 3. 返回一个自包含的、匿名的 AsyncGenerator 实现
            return new AsyncGenerator<NodeOutput>() {
                private Data<NodeOutput> endMarker = null;

                @Override
                public Data<NodeOutput> next() {
                    if (endMarker != null) {
                        return endMarker;
                    }
                    try {
                        // 4. 从队列中取出数据，如果队列为空则阻塞等待
                        @SuppressWarnings("unchecked")
                        Data<NodeOutput> value = (Data<NodeOutput>) queue.take();
                        if (value.isDone()) {
                            endMarker = value;
                        }
                        return value;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return AsyncGenerator.Data.error(e);
                    }
                }
            };
        }
    }

}
