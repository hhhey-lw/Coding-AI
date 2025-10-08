package com.coding.graph.core.generator;

import lombok.Getter;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * 异步生成器接口
 * 提供异步数据迭代功能
 * @param <E> 迭代元素类型
 */
public interface AsyncGenerator<E> extends Iterable<E> {

    // 获取下一个迭代元素
    Data<E> next();

    // 迭代方法
    default Iterator<E> iterator() {
        return new InternalIterator<E>(this);
    }

    // 提供迭代的流式访问
    default Stream<E> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    default CompletableFuture<Object> forEachAsync(Consumer<E> consumer) {
        CompletableFuture<Object> future = completedFuture(null);
        for (AsyncGenerator.Data<E> next = next(); !next.isDone(); next = next()) {
            final AsyncGenerator.Data<E> finalNext = next;
            future = future.thenCompose(v -> finalNext.data.thenAcceptAsync(consumer, Runnable::run).thenApply(x -> null));
        }
        return future;
    }

    // 包装类，包含异步数据和结果值
    @Getter
    class Data<E> {
        final CompletableFuture<E> data;

        final Object resultValue;

        // 构造函数
        private Data(CompletableFuture<E> data, Object resultValue) {
            this.data = data;
            this.resultValue = resultValue;
        }

        public boolean isDone() {
            return data == null;
        }

        public boolean isError() {
            return data !=null && data.isCompletedExceptionally();
        }

        public static <E> Data<E> of(CompletableFuture<E> data) {
            return new Data<>(data, null);
        }

        public static <E> Data<E> of(E data)  {
            return new Data<>(completedFuture(data), null);
        }

        public static <E> Data<E> error(Throwable e) {
            CompletableFuture<E> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return Data.of(future);
        }

        public static <E> Data<E> done(Object resultValue) {
            return new Data<>(null, resultValue);
        }

    }

    // 带有结果值的包装类
    class WithResult<E> implements AsyncGenerator<E> {

        protected final AsyncGenerator<E> delegate;

        private Object resultValue;

        public WithResult(AsyncGenerator<E> delegate) {
            this.delegate = delegate;
        }

        public AsyncGenerator<E> delegate() {
            return delegate;
        }

        public Optional<Object> resultValue() {
            return ofNullable(resultValue);
        };

        @Override
        public final Data<E> next() {
            final Data<E> result = delegate.next();
            if (result.isDone()) {
                resultValue = result.resultValue;
            }
            return result;
        }

    }

    // 内部迭代器
    class InternalIterator<E> implements Iterator<E> {

        private final AsyncGenerator<E> delegate;

        final AtomicReference<AsyncGenerator.Data<E>> currentFetchedData;

        public InternalIterator(AsyncGenerator<E> delegate) {
            this.delegate = delegate;
            currentFetchedData = new AtomicReference<>(delegate.next());
        }

        @Override
        public boolean hasNext() {
            final var value = currentFetchedData.get();
            return value != null && !value.isDone();
        }

        @Override
        public E next() {
            var next = currentFetchedData.get();

            if (next == null || next.isDone()) {
                throw new IllegalStateException("没有更多元素可供迭代");
            }

            if (!next.isError()) {
                currentFetchedData.set(delegate.next());
            }

            return next.data.join();
        }
    }

}