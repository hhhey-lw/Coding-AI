package com.coding.graph.core.generator;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * 异步生成器接口
 * 提供异步数据迭代功能
 * @param <E> 迭代元素类型
 */
public interface AsyncGenerator<E> extends Iterable<E>, AsyncGeneratorOperators<E> {

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

    /**
     * 包装
     * 异步生成器
     * 完成回调
     */
    class Embed<E> {

        @Getter
        final AsyncGenerator<E> generator;

        final EmbedCompletionHandler onCompletion;

        public Embed(AsyncGenerator<E> generator, EmbedCompletionHandler onCompletion) {
            Objects.requireNonNull(generator, "generator cannot be null");
            this.generator = generator;
            this.onCompletion = onCompletion;
        }

    }

    @FunctionalInterface
    interface EmbedCompletionHandler {

        void accept(Object t) throws Exception;

    }

    // 包装类，包含异步数据和结果值
    @Getter
    class Data<E> {
        final CompletableFuture<E> data;

        final Embed<E> embed;

        final Object resultValue;

        // 构造函数
        private Data(CompletableFuture<E> data, Object resultValue) {
            this.data = data;
            this.resultValue = resultValue;
            this.embed = null;
        }

        public Data(CompletableFuture<E> data, Embed<E> embed, Object resultValue) {
            this.data = data;
            this.embed = embed;
            this.resultValue = resultValue;
        }

        public boolean isDone() {
            return data == null && embed == null;
        }

        public boolean isError() {
            return data !=null && data.isCompletedExceptionally();
        }

        public static <E> Data<E> of(CompletableFuture<E> data) {
            return new Data<>(data, null, null);
        }

        public static <E> Data<E> of(E data)  {
            return new Data<>(completedFuture(data), null, null);
        }

        /**
         * 完成时触发，写入嵌套生成器的最终输出
         */
        public static <E> Data<E> composeWith(AsyncGenerator<E> generator, EmbedCompletionHandler onCompletion) {
            return new Data<>(null, new Embed<>(generator, onCompletion), null);
        }

        public static <E> Data<E> error(Throwable e) {
            CompletableFuture<E> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return Data.of(future);
        }

        public static <E> Data<E> done(Object resultValue) {
            return new Data<>(null, null, resultValue);
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

    // 能够嵌套生成器的包装类
    class WithEmbed<E> implements AsyncGenerator<E> {
        // 存储嵌套生成器的栈：1. 节点迭代生成器 >> 2. 流式输出的生成器
        protected final Deque<Embed<E>> generatorsStack = new ArrayDeque<>(2);
        // 存储返回值的栈
        private final Deque<Data<E>> returnValueStack = new ArrayDeque<>(2);

        public WithEmbed(AsyncGenerator<E> delegate, EmbedCompletionHandler onGeneratorDoneWithResult) {
            generatorsStack.push(new Embed<>(delegate, onGeneratorDoneWithResult));
        }

        public WithEmbed(AsyncGenerator<E> delegate) {
            this(delegate, null);
        }

        public Optional<Object> resultValue() {
            return ofNullable(returnValueStack.peek()).map(r -> r.resultValue);
        }

        private void clearPreviousReturnsValuesIfAny() {
            // Check if the return values are which ones from previous run
            if (returnValueStack.size() > 1 && returnValueStack.size() == generatorsStack.size()) {
                returnValueStack.clear();
            }
        }

        protected boolean isLastGenerator() {
            return generatorsStack.size() == 1;
        }

        @Override
        public Data<E> next() {
            if (generatorsStack.isEmpty()) { // GUARD
                throw new IllegalStateException("no generator found!");
            }
            final Embed<E> embed = generatorsStack.peek();
            final Data<E> result = embed.generator.next();

            if (result.isDone()) {
                clearPreviousReturnsValuesIfAny();
                returnValueStack.push(result);
                if (embed.onCompletion != null /* && result.resultValue != null */ ) {
                    try {
                        embed.onCompletion.accept(result.resultValue);
                    }
                    catch (Exception e) {
                        return Data.error(e);
                    }
                }
                if (isLastGenerator()) {
                    return result;
                }
                generatorsStack.pop();
                return next();
            }
            if (result.embed != null) {
                if (generatorsStack.size() >= 2) {
                    return Data.error(new UnsupportedOperationException(
                            "Currently recursive nested generators are not supported!"));
                }
                generatorsStack.push(result.embed);
                return next();
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

    default AsyncGeneratorOperators<E> async(Executor executor) {
        return new AsyncGeneratorOperators<E>() {
            @Override
            public Data<E> next() {
                return AsyncGenerator.this.next();
            }

            @Override
            public Executor executor() {
                return executor;
            }
        };
    }

}