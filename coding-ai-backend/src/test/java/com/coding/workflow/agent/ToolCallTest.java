package com.coding.workflow.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest()
public class ToolCallTest {

    @Resource
    private ChatModel chatModel;

    @Test
    public void test() throws InterruptedException {
        ChatClient chatClient = ChatClient.builder(chatModel)
                // 实现 Chat Memory 的 Advisor
                // 在使用 Chat Memory 时，需要指定对话 ID，以便 Spring AI 处理上下文。
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

        UserMessage userMessage = new UserMessage("""
                Hi, 请你生成一张海边落日的图片，参考图是 https://example.com/reference.jpg.
                此外，你还需要生成响应的音乐，风格和歌词你自由创作。
                """);

        Flux<ChatResponse> chatResponse = chatClient.prompt()
                .messages(userMessage)
                .toolCallbacks(FunctionToolCallback.builder("generateMusic", new MusicGenerateService())
                        .description("根据歌词和风格提示词，生成一段音乐，并返回音乐的URL地址")
                        .inputType(MusicGenerateService.Request.class)
                        .build(), FunctionToolCallback.builder("generateImage", new ImageGenerateService())
                        .description("根据提示词和参考图生成图片，并返回图片的URL地址")
                        .inputType(ImageGenerateService.Request.class)
                        .build())
                .stream()
                .chatResponse();

        chatResponse.subscribe(System.out::println);
        Thread.sleep(10000);

//        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
//        AssistantMessage.ToolCall toolCall = assistantMessage.getToolCalls().get(0);
//        ToolResponseMessage toolResponseMessage = new ToolResponseMessage(List.of(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), """
//                {
//                    "url": "https://example.com/generated_image.png"
//                }
//                """)));
//
//        chatResponse = chatClient.prompt()
//                        .messages(List.of(
//                                 userMessage, assistantMessage, toolResponseMessage
//                        ))
//                                .call().chatResponse();
//
//
//        System.out.println(chatResponse);
    }

    static class CustomerTool {

        @Tool(description = "根据提示词和歌词，生成一段音乐，并返回音乐的URL地址")
        String generateMusic(@ToolParam(description = "描述音乐的风格") String prompt,
                             @ToolParam(description = "歌词内容") String lyrics) {
            return "https://example.com/music.mp3";
        }

        @Tool(description = "根据提示词和参考图生成图片，并返回图片的URL地址")
        String generateImage(@ToolParam(description = "生成图片的提示词") String prompt,
                             @ToolParam(description = "参考图URL地址") String referenceImage) {
            return "https://example.com/image.png";
        }
    }

}
