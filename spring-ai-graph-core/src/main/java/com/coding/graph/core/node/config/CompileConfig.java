package com.coding.graph.core.node.config;

import com.coding.graph.core.graph.GraphLifecycleListener;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class CompileConfig {

    // 生命周期监听器
    private Deque<GraphLifecycleListener> lifecycleListeners = new LinkedBlockingDeque<>(25);

    // 构造函数
    public CompileConfig() {
    }

    private CompileConfig(CompileConfig config) {
        this.lifecycleListeners = config.lifecycleListeners;
    }

    public static Builder builder() {
        return new Builder(new CompileConfig());
    }

    // 获取生命周期监听器队列
    public Queue<GraphLifecycleListener> lifecycleListeners() {
        return this.lifecycleListeners;
    }

    /**
     * 构建器
     */
    public static class Builder {
        private final CompileConfig config;

        protected Builder(CompileConfig config) {
            this.config = new CompileConfig(config);
        }

        public Builder withLifecycleListener(GraphLifecycleListener listener) {
            this.config.lifecycleListeners.offer(listener);
            return this;
        }

        public CompileConfig build() {
            return config;
        }
    }
}
