package com.coding.graph.core.streaming;

import com.coding.graph.core.generator.AsyncGenerator;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

/**
 * 用于生成异步数据流的订阅者。
 *
 * <p>
 * 该类实现了 {@link Flow.Subscriber} 和 {@link AsyncGenerator} 接口，
 * 用于处理数据流并生成异步数据。它被设计用于订阅发布者、
 * 处理传入的项目，并管理错误和完成信号。
 * </p>
 *
 * @param <T> 此生成器产生的元素类型。
 */
public class GeneratorSubscriber<T> implements Flow.Subscriber<T>, AsyncGenerator<T> {

	private final AsyncGeneratorQueue.Generator<T> delegate;

	private final Supplier<Object> mapResult;

	public Optional<Supplier<Object>> mapResult() {
		return Optional.ofNullable(mapResult);
	}

	/**
	 * 构造一个新的 {@code GeneratorSubscriber} 实例。
	 * @param <P> 发布者的类型，必须扩展 {@link Flow.Publisher}
	 * @param mapResult 用于设置生成器结果的函数
	 * @param publisher 将向此订阅者推送数据的源发布者
	 * @param queue 用于存储异步生成器数据的阻塞队列
	 */
	public <P extends Flow.Publisher<T>> GeneratorSubscriber(P publisher, Supplier<Object> mapResult,
			BlockingQueue<Data<T>> queue) {
		this.delegate = new AsyncGeneratorQueue.Generator<>(queue);
		this.mapResult = mapResult;
		publisher.subscribe(this);
	}

	/**
	 * 构造一个新的 {@code GeneratorSubscriber} 实例。
	 * @param <P> 发布者的类型，必须扩展 {@link Flow.Publisher}
	 * @param publisher 将向此订阅者推送数据的源发布者
	 * @param queue 用于存储异步生成器数据的阻塞队列
	 */
	public <P extends Flow.Publisher<T>> GeneratorSubscriber(P publisher, BlockingQueue<Data<T>> queue) {
		this(publisher, null, queue);
	}

	/**
	 * 处理来自 Flux 的订阅事件。
	 * <p>
	 * 当与源 {@link Flow} 的订阅已建立时，将调用此方法。
	 * 提供的 {@code Flow.Subscription} 可用于管理和控制数据发射流。
	 * @param subscription 表示此资源所有者生命周期的订阅对象。用于发出信号，
	 * 表示在释放此订阅之前不应释放被订阅的资源。
	 */
	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		subscription.request(Long.MAX_VALUE);
	}

	/**
	 * 将接收到的项目作为 {@link Data} 对象传递给委托队列。
	 * @param item 要处理并加入队列的项目。
	 */
	@Override
	public void onNext(T item) {
		delegate.queue().add(Data.of(item));
	}

	/**
	 * 通过在委托队列中添加错误数据来处理错误。
	 * @param error 表示要处理的错误的 Throwable。
	 */
	@Override
	public void onError(Throwable error) {
		delegate.queue().add(Data.error(error));
	}

	/**
	 * 当异步操作成功完成时调用此方法。它通过向队列添加完成标记来
	 * 通知委托不再提供更多数据。
	 */
	@Override
	public void onComplete() {
		delegate.queue().add(Data.done(mapResult().map(Supplier::get).orElse(null)));
	}

	/**
	 * 从此迭代中返回下一个 {@code Data<T>} 对象。
	 * @return 迭代中的下一个元素，如果没有这样的元素则返回 null
	 */
	@Override
	public Data<T> next() {
		return delegate.next();
	}

}
