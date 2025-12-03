package com.coding.agentflow.service.node;

import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import com.coding.core.manager.tool.ImageGenerateService;
import com.coding.core.manager.tool.MusicGenerateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具节点 TODO 待优化
 * 调用外部工具或API执行特定功能
 */
@Slf4j
@Component
public class ToolNode extends AbstractNode {

    private final MusicGenerateService musicGenerateService;
    private final ImageGenerateService imageGenerateService;

    public ToolNode(MusicGenerateService musicGenerateService,
                    ImageGenerateService imageGenerateService) {
        this.musicGenerateService = musicGenerateService;
        this.imageGenerateService = imageGenerateService;
    }

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) {
        // 获取配置参数
        String toolName = getConfigParamAsString(node, "toolName");
        Map<String, Object> toolParams = getConfigParamAsMap(node, "toolParams");

        // 支持从上下文中替换参数变量
        Map<String, Object> resolvedParams = resolveParams(toolParams, state);

        log.info("执行工具节点，工具名称: {}, 参数: {}", toolName, JSONUtil.toJsonStr(resolvedParams));

        // 根据工具类型调用相应的工具
        Object toolResult = invokeTool(toolName, resolvedParams, state);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("toolName", toolName);
        resultData.put("toolParams", resolvedParams);
        resultData.put("toolResult", toolResult);

        return resultData;
    }

    /**
     * 调用工具
     * 
     * @param toolName 工具名称
     * @param params 工具参数
     * @param state 执行状态
     * @return 工具执行结果
     */
    private Object invokeTool(String toolName, Map<String, Object> params, OverAllState state) {
        if (StringUtils.isBlank(toolName)) {
            throw new IllegalArgumentException("工具名称不能为空");
        }

        try {
            switch (toolName) {
                case "generateMusic":
                    return invokeGenerateMusic(params);
                case "generateImage":
                    return invokeGenerateImage(params);
                default:
                    log.error("不支持的工具类型: {}", toolName);
                    return Map.of(
                            "status", "error",
                            "message", "不支持的工具类型: " + toolName
                    );
            }
        } catch (Exception e) {
            log.error("工具调用失败: {}", toolName, e);
            return Map.of(
                    "status", "error",
                    "message", "工具调用失败: " + e.getMessage()
            );
        }
    }

    /**
     * 调用音乐生成工具
     */
    private Object invokeGenerateMusic(Map<String, Object> params) {
        String prompt = (String) params.get("prompt");
        String lyrics = (String) params.get("lyrics");

        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("音乐生成缺少必需参数: prompt");
        }
        if (StringUtils.isBlank(lyrics)) {
            throw new IllegalArgumentException("音乐生成缺少必需参数: lyrics");
        }

        log.info("调用音乐生成服务，prompt: {}, lyrics: {}", prompt, lyrics);
        MusicGenerateService.Request request = new MusicGenerateService.Request(prompt, lyrics);
        MusicGenerateService.Response response = musicGenerateService.apply(request);

        return Map.of(
                "status", "success",
                "message", "音乐生成成功",
                "data", Map.of(
                        "musicUrl", response.musicUrl(),
                        "prompt", prompt,
                        "lyrics", lyrics
                )
        );
    }

    /**
     * 调用图片生成工具
     */
    private Object invokeGenerateImage(Map<String, Object> params) {
        String prompt = (String) params.get("prompt");
        String referenceImage = (String) params.get("referenceImage");

        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("图片生成缺少必需参数: prompt");
        }

        log.info("调用图片生成服务，prompt: {}, referenceImage: {}", prompt, referenceImage);
        ImageGenerateService.Request request = new ImageGenerateService.Request(prompt, referenceImage);
        ImageGenerateService.Response response = imageGenerateService.apply(request);

        return Map.of(
                "status", "success",
                "message", "图片生成成功",
                "data", Map.of(
                        "imageUrl", response.imageUrl(),
                        "prompt", prompt,
                        "referenceImage", referenceImage != null ? referenceImage : ""
                )
        );
    }

    /**
     * 解析参数，支持从上下文中替换变量
     */
    private Map<String, Object> resolveParams(Map<String, Object> params, OverAllState state) {
        if (params == null || params.isEmpty()) {
            return params;
        }

        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                // 支持变量替换
                String resolvedValue = replaceTemplateWithVariable((String) value, state);
                resolved.put(entry.getKey(), resolvedValue);
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String toolName = getConfigParamAsString(node, "toolName");
        if (StringUtils.isBlank(toolName)) {
            log.error("工具节点缺少必需的toolName配置");
            return false;
        }

        // 验证工具名称是否支持
        if (!"generateMusic".equals(toolName) && !"generateImage".equals(toolName)) {
            log.error("不支持的工具类型: {}", toolName);
            return false;
        }

        // 验证工具参数
        Object toolParamsObj = getConfigParam(node, "toolParams");
        if (toolParamsObj == null) {
            log.error("工具节点缺少必需的toolParams配置");
            return false;
        }

        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.TOOL.name();
    }
}
