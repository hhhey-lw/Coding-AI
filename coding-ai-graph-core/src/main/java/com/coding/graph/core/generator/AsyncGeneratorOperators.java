package com.coding.graph.core.generator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;

public interface AsyncGeneratorOperators<E> {

	AsyncGenerator.Data<E> next();

	default Executor executor() {
		return Runnable::run;
	}

	/**
	 * 将当前生成器的元素映射为一个新的异步生成器。
	 *
	 * 该方法接收一个映射函数，将当前生成器中的每个元素转换为另一种类型的异步元素，
	 * 并返回一个新的异步生成器，该生成器将生成经过映射后的元素。
	 *
	 * @param mapFunction 用于将元素映射为异步对应元素的函数。
	 *                   该函数定义了如何将当前生成器的元素类型 T 转换为目标生成器的元素类型 U。
	 * @param <U>         新生成器中元素的类型，即映射后的目标类型。
	 * @return            一个异步生成器，其元素类型为 U，元素内容由 mapFunction 映射而来。
	 */
	default <U> AsyncGenerator<U> map(Function<E, U> mapFunction) {
		return () -> {
			final AsyncGenerator.Data<E> next = next();
			if (next.isDone()) {
				return AsyncGenerator.Data.done(next.resultValue);
			}
			return AsyncGenerator.Data.of(next.data.thenApplyAsync(mapFunction, executor()));
		};
	}

	/**
	 * 异步迭代 AsyncGenerator 的元素，并将给定的消费者应用于每个元素。
	 *
	 * ⚠️ 注意：此方法会在调用线程中阻塞等待所有数据产生完毕后才返回，
	 * 不适合需要实时流式处理的场景。对于实时流式处理，请使用 streamForEach 方法。
	 * 
	 * @param consumer 应用于每个元素的消费者函数
	 * @return 表示迭代过程完成的 CompletableFuture
	 */
	default CompletableFuture<Object> forEachAsync(Consumer<E> consumer) {
		CompletableFuture<Object> future = completedFuture(null);
		for (AsyncGenerator.Data<E> next = next(); !next.isDone(); next = next()) {
			final AsyncGenerator.Data<E> finalNext = next;
			if (finalNext.embed != null) {
				future = future.thenCompose(v -> finalNext.embed.generator.async(executor()).forEachAsync(consumer));
			}
			else {
				future = future
						.thenCompose(v -> finalNext.data.thenAcceptAsync(consumer, executor()).thenApply(x -> null));
			}
		}
		return future;
	}


}
