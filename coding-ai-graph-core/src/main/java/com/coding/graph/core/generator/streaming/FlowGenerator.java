package com.coding.graph.core.generator.streaming;

import com.coding.graph.core.generator.AsyncGenerator;

import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * 提供在 {@link FlowGenerator} 和各种 {@link Flow.Publisher} 类型之间进行转换的方法。
 *
 * @since 3.0.0
 */
public interface FlowGenerator {

	/**
	 * 从 {@code Flow.Publisher} 创建 {@code AsyncGenerator}。
	 * @param <T> 发布者发出的项目类型
	 * @param <P> 发布者的类型
	 * @param publisher 要订阅以异步检索项目的发布者
	 * @param mapResult 用于设置生成器结果的函数
	 * @return 从发布者发出项目的 {@code AsyncGenerator}
	 */
	@SuppressWarnings("unchecked")
	static <T, P extends Flow.Publisher<T>, R> AsyncGenerator<T> fromPublisher(P publisher, Supplier<R> mapResult) {
		var queue = new LinkedBlockingQueue<AsyncGenerator.Data<T>>();
		return new GeneratorSubscriber<>(publisher, (Supplier<Object>) mapResult, queue);
	}

	/**
	 * 从 {@code Flow.Publisher} 创建 {@code AsyncGenerator}。
	 * @param <T> 发布者发出的项目类型
	 * @param <P> 发布者的类型
	 * @param publisher 要订阅以异步检索项目的发布者
	 * @return 从发布者发出项目的 {@code AsyncGenerator}
	 */
	static <T, P extends Flow.Publisher<T>> AsyncGenerator<T> fromPublisher(P publisher) {
		return fromPublisher(publisher, null);
	}

}
