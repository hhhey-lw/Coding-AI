package com.coding.graph.core.streaming;

import com.coding.graph.core.generator.AsyncGenerator;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * 表示基于队列的异步生成器。
 */
public class AsyncGeneratorQueue {

	/**
	 * 从队列生成异步元素的内部类。
	 *
	 * @param <E> 队列中元素的类型
	 */
	public static class Generator<E> implements AsyncGenerator<E> {

		Data<E> isEnd = null;

		final BlockingQueue<Data<E>> queue;

		/**
		 * 使用指定的队列构造生成器。
		 * @param queue 用于生成元素的阻塞队列
		 */
		public Generator(BlockingQueue<Data<E>> queue) {
			this.queue = queue;
		}

		public BlockingQueue<Data<E>> queue() {
			return queue;
		}

		/**
		 * 异步从队列中检索下一个元素。
		 * @return 队列中的下一个元素
		 */
		@Override
		public Data<E> next() {
            try {
                while (isEnd == null) {
                    Data<E> value = queue.take();
                    if (value != null) {
                        if (value.isDone()) {
                            isEnd = value;
                        }
                        return value;
                    }
                }
                return isEnd;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

	}

	/**
	 * 从提供的阻塞队列和消费者创建 AsyncGenerator。
	 * @param <E> 队列中元素的类型
	 * @param <Q> 阻塞队列的类型
	 * @param queue 用于生成元素的阻塞队列
	 * @param consumer 用于处理队列中元素的消费者
	 * @return AsyncGenerator 实例
	 */
	public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue,
			Consumer<Q> consumer) {
		return of(queue, consumer, commonPool());
	}

	/**
	 * 从提供的队列、执行器和消费者创建 AsyncGenerator。
	 * @param <E> 队列中元素的类型
	 * @param <Q> 阻塞队列的类型
	 * @param queue 用于生成元素的阻塞队列
	 * @param consumer 用于处理队列中元素的消费者
	 * @param executor 用于异步处理的执行器
	 * @return AsyncGenerator 实例
	 */
	public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue,
			Consumer<Q> consumer, Executor executor) {
		Objects.requireNonNull(queue);
		Objects.requireNonNull(executor);
		Objects.requireNonNull(consumer);

		executor.execute(() -> {
			try {
				consumer.accept(queue);
			}
			catch (Throwable ex) {
				CompletableFuture<E> error = new CompletableFuture<>();
				error.completeExceptionally(ex);
				queue.add(AsyncGenerator.Data.of(error));
			}
			finally {
				queue.add(AsyncGenerator.Data.done(null));
			}

		});

		return new Generator<>(queue);
	}

}
