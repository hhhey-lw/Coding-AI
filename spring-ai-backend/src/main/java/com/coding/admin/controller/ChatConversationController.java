package com.coding.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.admin.common.Result;
import com.coding.admin.model.model.ChatConversationModel;
import com.coding.admin.model.request.ChatConversationCreateRequest;
import com.coding.admin.model.request.ChatConversationPageRequest;
import com.coding.admin.model.response.ChatConversationDetailResponse;
import com.coding.admin.model.vo.ChatConversationVO;
import com.coding.admin.model.vo.PageVO;
import com.coding.admin.model.vo.SimpleChatMessageVO;
import com.coding.admin.service.ChatConversationService;
import com.coding.admin.service.ChatMessageService;
import com.coding.admin.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话控制器
 * @author coding
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/ai/conversation")
@Tag(name = "聊天会话管理", description = "聊天会话的增删改查接口")
public class ChatConversationController {

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 创建会话
     */
    @PostMapping("/create")
    @Operation(summary = "创建会话", description = "创建新的聊天会话")
    public Result<String> createConversation(@RequestBody @Validated ChatConversationCreateRequest request) {
        log.info("创建会话，title: {}", request.getTitle());
        
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        
        // 构建Model
        ChatConversationModel model = new ChatConversationModel();
        model.setUserId(userId != null ? userId.toString() : null);
        model.setTitle(request.getTitle());
        
        // 如果请求中指定了ID，则使用；否则自动生成
        if (request.getId() != null && !request.getId().isEmpty()) {
            model.setId(request.getId());
        } else {
            model.setId(IdWorker.getIdStr());
        }
        
        // 创建会话
        String conversationId = chatConversationService.createConversation(model);
        
        return Result.success("会话创建成功", conversationId);
    }

    /**
     * 分页获取会话列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询会话列表", description = "分页查询当前用户的聊天会话")
    public Result<PageVO<ChatConversationVO>> getConversationPage(@Validated ChatConversationPageRequest request) {
        log.info("分页查询会话列表，pageNum: {}, pageSize: {}, status: {}", 
                 request.getPageNum(), request.getPageSize(), request.getStatus());
        
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        // 分页查询
        Page<ChatConversationVO> page = chatConversationService.pageByUserId(
                userId.toString(), 
                request.getStatus(), 
                request.getPageNum(), 
                request.getPageSize()
        );
        
        // 构建分页响应
        PageVO<ChatConversationVO> pageVO = PageVO.of(
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getTotal(),
                page.getRecords()
        );
        
        return Result.success("查询成功", pageVO);
    }

    /**
     * 拉取会话详情（包含消息列表）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询会话详情", description = "根据ID获取会话详情及消息列表")
    public Result<ChatConversationDetailResponse> getConversationDetail(
            @Parameter(description = "会话ID") @PathVariable String id) {
        log.info("查询会话详情，id: {}", id);
        
        // 查询会话信息
        ChatConversationVO conversation = chatConversationService.getById(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        
        // 查询消息列表
        List<Message> messages = chatMessageService.findMessages(id);
        
        // 将 Message 转换为 SimpleChatMessageVO（前端友好格式）
        List<SimpleChatMessageVO> simpleMessages = messages.stream()
                .map(this::convertToSimpleMessageVO)
                .collect(Collectors.toList());
        
        // 构建响应
        ChatConversationDetailResponse response = ChatConversationDetailResponse.builder()
                .conversation(conversation)
                .messages(simpleMessages)
                .build();
        
        return Result.success("查询成功", response);
    }
    
    /**
     * 将 Spring AI Message 转换为前端友好的 SimpleChatMessageVO
     * 支持 React Agent 和 Planning Agent 的完整消息格式
     */
    private SimpleChatMessageVO convertToSimpleMessageVO(Message message) {
        SimpleChatMessageVO.SimpleChatMessageVOBuilder builder = SimpleChatMessageVO.builder();
        
        if (message instanceof UserMessage) {
            builder.role("user")
                   .content(message.getText());
                   
        } else if (message instanceof AssistantMessage assistantMsg) {
            builder.role("assistant")
                   .content(message.getText() != null ? message.getText() : "");
            
            // 如果有工具调用，转换工具调用信息（与前端格式保持一致）
            if (assistantMsg.getToolCalls() != null && !assistantMsg.getToolCalls().isEmpty()) {
                List<com.coding.admin.model.vo.ToolCallVO> toolCalls = assistantMsg.getToolCalls().stream()
                        .map(tc -> com.coding.admin.model.vo.ToolCallVO.builder()
                                .id(tc.id())
                                .name(tc.name())
                                .arguments(tc.arguments())
                                .build())
                        .collect(Collectors.toList());
                builder.toolCalls(toolCalls);
            }
            
        } else if (message instanceof SystemMessage) {
            builder.role("system")
                   .content(message.getText());
                   
        } else if (message instanceof ToolResponseMessage toolMessage) {
            builder.role("tool")
                   .content(""); // tool 消息的内容在 responses 中
            
            // 转换工具响应信息
            if (toolMessage.getResponses() != null && !toolMessage.getResponses().isEmpty()) {
                List<com.coding.admin.model.vo.ToolResponseVO> responses = toolMessage.getResponses().stream()
                        .map(tr -> com.coding.admin.model.vo.ToolResponseVO.builder()
                                .id(tr.id())
                                .name(tr.name())
                                .responseData(tr.responseData())
                                .build())
                        .collect(Collectors.toList());
                builder.responses(responses);
            }
            
        } else {
            builder.role("unknown")
                   .content(message.getText() != null ? message.getText() : "");
        }
        
        return builder.build();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话", description = "根据ID删除会话（软删除）")
    public Result<Boolean> deleteConversation(
            @Parameter(description = "会话ID") @PathVariable String id) {
        log.info("删除会话，id: {}", id);
        
        // 查询会话是否存在
        ChatConversationVO conversation = chatConversationService.getById(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        
        // 验证权限：只能删除自己的会话
        Long userId = UserContextHolder.getUserId();
        if (userId != null && conversation.getUserId() != null 
                && !userId.toString().equals(conversation.getUserId())) {
            return Result.error("无权限删除该会话");
        }
        
        // 删除会话（软删除）
        int result = chatConversationService.deleteConversation(id);
        
        return result > 0 ? Result.success("删除成功", true) : Result.error("删除失败");
    }
}
