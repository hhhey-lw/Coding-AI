package com.coding.graph.core.utils;

import com.coding.graph.core.graph.GraphLifecycleListener;
import com.coding.graph.core.node.config.RunnableConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.Map;

import static com.coding.graph.core.common.NodeCodeConstants.*;

@Slf4j
public class LifeListenerUtil {

    public static void processListenersLIFO(String currentNodeId, Deque<GraphLifecycleListener> listeners,
                                            Map<String, Object> currentState, RunnableConfig runnableConfig, String scene, Throwable e) {
        if (listeners.isEmpty()) {
            return;
        }

        GraphLifecycleListener listener = listeners.pollLast();

        try {
            // 调用适当的监听器方法
            switch (scene) {
                case START -> listener.onStart(START, currentState, runnableConfig);
                case END -> listener.onComplete(END, currentState, runnableConfig);
                case ERROR -> listener.onError(ERROR, currentState, e, runnableConfig);
                case NODE_BEFORE -> listener.before(currentNodeId, currentState, runnableConfig, System.currentTimeMillis());
                case NODE_AFTER -> listener.after(currentNodeId, currentState, runnableConfig, System.currentTimeMillis());
            }
            // 递归处理下一个监听器
            processListenersLIFO(currentNodeId, listeners, currentState, runnableConfig, scene, e);
        } catch (Exception ex) {
            log.debug("监听器处理过程出现异常：{}", ex.getMessage(), ex);
        }
    }

}
