package com.coding.admin.manager;

import com.coding.admin.manager.tool.ImageGenerateService;
import com.coding.admin.manager.tool.MusicGenerateService;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.node.impl.LlmNode;
import com.coding.graph.core.node.impl.ToolNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentManager {

    private final ChatClient chatClient;

    private final ToolCallbackResolver resolver;

    public AgentManager(ChatModel chatModel, ToolCallbackResolver resolver) {
        this.resolver = resolver;
        this.chatClient = ChatClient.builder(chatModel)
                // 实现 Chat Memory 的 Advisor
                // 在使用 Chat Memory 时，需要指定对话 ID，以便 Spring AI 处理上下文。  好坑啊：!!! 这个功能会多生成会话消息
//                .defaultAdvisors(
//                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build()
//                )
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .internalToolExecutionEnabled(false)
                                .parallelToolCalls(false)
                                .model("qwen-max")
                                .build()
                )
                .build();
    }

    public ReactAgent buildAgent() throws GraphStateException {
        List<ToolCallback> toolCallbacks = List.of(FunctionToolCallback.builder("generateMusic", new MusicGenerateService())
                .description("根据提示词和歌词，生成一段音乐，并返回音乐的URL地址")
                .inputType(MusicGenerateService.Request.class)
                .build(), FunctionToolCallback.builder("generateImage", new ImageGenerateService())
                .description("根据提示词和参考图生成图片，并返回图片的URL地址")
                .inputType(ImageGenerateService.Request.class)
                .build());

        ReactAgent.Builder builder = ReactAgent.Builder.builder()
                .name("react-agent")
                .inputKey("messages")
                .resolver(resolver)
                .maxIterations(10)
                .build();

        LlmNode llmNode = LlmNode.builder()
                .systemPrompt("""
                        你是一个乐于助人的AI小助手，能够帮助用户完成任务。
                        你必须严格按照以下格式输出你的内容，不能省略任何部分：
                        
                        格式要求：
                        ---
                        思考: <你的推理过程，说明你为什么这么做，是否要调用工具，调用哪个工具等>
                        行动: <你的下一步行动>
                        
                        行动可以是：
                        1. 调用一个工具，格式为：行动: 工具名(参数1=值1, 参数2=值2, ...)
                        2. 直接回答用户，格式为：行动: <你的回答内容>
                        
                        你必须在每次回复中都包含 思考，即使你决定直接回答。
                        
                        不允许只输出内容或工具调用，必须说明你的思考过程！也就是即使你准备调用工具，你也必须说明你的思考。
                        """)
                .chatClient(chatClient)
                .toolCallbacks(toolCallbacks)
                .model("qwen-max")
                .messagesKey("messages")
                .stream(true)
                .build();

        ToolNode toolNode = ToolNode.builder()
                .llmResponseKey(LlmNode.LLM_RESPONSE_KEY)
                .toolCallbackResolver(resolver)
                .toolCallbacks(toolCallbacks)
                .build();

        return new ReactAgent(llmNode, toolNode, builder);
    }

}
