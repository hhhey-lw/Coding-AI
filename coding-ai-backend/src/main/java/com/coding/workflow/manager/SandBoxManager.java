package com.coding.workflow.manager;

import cn.hutool.json.JSONUtil;
import com.coding.core.common.Result;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SandBoxManager {

    private static final String OUTPUT_DECORATE_PARAM_KEY = "output";

    private static final String SCRIPT_TYPE_PYTHON = "python";
    private static final String SCRIPT_TYPE_JS = "js";

    public Result<String> executePythonScript(String scriptContent, Map<String, Object> variables, String requestId) {
        return executeScript(SCRIPT_TYPE_PYTHON, scriptContent, variables, requestId);
    }

    public Result<String> executeJavaScript(String scriptContent, Map<String, Object> variables, String requestId) {
        return executeScript(SCRIPT_TYPE_JS, scriptContent, variables, requestId);
    }

    private Result<String> executeScript(String scriptType, String scriptContent, Map<String, Object> variables, String requestId) {
        try {
            Context.Builder builder = Context.newBuilder(scriptType)
                    .allowAllAccess(true);

            if (SCRIPT_TYPE_PYTHON.equals(scriptType)) {
                builder.option("python.ForceImportSite", "true");
            }

            try (Context context = builder.build()) {
                // 1. 注入变量到执行环境中
                if (variables != null && !variables.isEmpty()) {
//                    context.getBindings(scriptType).putMember("params", variables);
                    for (Map.Entry<String, Object> entry : variables.entrySet()) {
                        context.getBindings(scriptType).putMember(entry.getKey(), entry.getValue());
                    }
                }

                // 2. 执行脚本并获取结果
                Value result = context.eval(scriptType, scriptContent);

                // 3. 构建返回结果
                HashMap<Object, Object> resultMap = Maps.newHashMap();
                resultMap.put("success", true);
                HashMap<Object, Object> innerMap = Maps.newHashMap();

                Object convertedResult = convertGraalvmValue(result);
                innerMap.put(OUTPUT_DECORATE_PARAM_KEY, convertedResult);
                resultMap.put("data", innerMap);

                // 4. 转为JSON格式返回
                return Result.success(requestId, JSONUtil.toJsonStr(resultMap));
            }
        } catch (Exception e) {
            log.error("GraalVM脚本执行异常, scriptType={}, requestId={}", scriptType, requestId, e);
            return Result.error("脚本执行异常, requestId=" + requestId);
        }
    }

    private Object convertGraalvmValue(Value result) {
        if (result.isNull()) {
            return null;
        }
        if (result.isString()) {
            return result.asString();
        }
        if (result.isNumber()) {
            return result.asDouble();
        }
        if (result.isBoolean()) {
            return result.asBoolean();
        }
        if (result.hasArrayElements()) {
            ArrayList<Object> list = Lists.newArrayList();
            for (long i = 0; i < result.getArraySize(); i++) {
                list.add(result.getArrayElement(i).as(Object.class));
            }
            return list;
        }
        if (result.hasMembers()) {
            return result.as(Map.class);
        }
        return null;
    }
}
