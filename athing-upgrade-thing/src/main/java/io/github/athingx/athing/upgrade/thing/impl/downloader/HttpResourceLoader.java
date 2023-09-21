package io.github.athingx.athing.upgrade.thing.impl.downloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/**
 * HTTP资源加载器
 */
public class HttpResourceLoader implements ResourceLoader {

    private final Option option;
    private final URI uri;

    /**
     * HTTP资源加载器
     *
     * @param option 加载选项
     * @param uri    资源URI
     */
    public HttpResourceLoader(Option option, URI uri) {
        this.option = option;
        this.uri = uri;
    }

    @Override
    public <T> CompletableFuture<T> load(Executor executor, long position, LoadingHandler<T> handler) {
        return CompletableFuture.runAsync(() -> handler.begin(position), executor)
                .thenCompose(unused -> {

                    // HTTP客户端
                    final var client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(option.getConnectTimeoutMs()))
                            .executor(executor)
                            .build();

                    // GET请求
                    final var request = HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofMillis(option.getTimeoutMs()))
                            .header("Range", "bytes=%s-".formatted(position))
                            .build();

                    // 异步下载
                    return client.sendAsync(request, BodyHandlers.buffering(new LoadingBodyHandler<>(handler), option.getBufferSize()))
                            .thenApply(response -> {
                                if (response.statusCode() != 200 && response.statusCode() != 206) {
                                    throw new IllegalStateException("http status: %s".formatted(response.statusCode()));
                                }
                                return response.body();
                            });

                });
    }

    /**
     * HTTP资源加载器
     *
     * @param handler 加载处理器
     * @param <T>     资源类型
     */
    private record LoadingBodyHandler<T>(LoadingHandler<T> handler) implements HttpResponse.BodyHandler<T> {

        @Override
        public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
            return new HttpResponse.BodySubscriber<>() {

                private final CompletableFuture<Void> future = new CompletableFuture<>();
                private volatile Flow.Subscription subscription;

                @Override
                public CompletionStage<T> getBody() {
                    return future.thenCompose(unused -> handler.getTarget());
                }

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(List<ByteBuffer> buffers) {
                    try {
                        for (final var buffer : buffers) {
                            handler.handle(buffer);
                        }
                        subscription.request(1);
                    } catch (IOException cause) {
                        onError(cause);
                    }
                }

                @Override
                public void onError(Throwable ex) {
                    if (future.completeExceptionally(ex)) {
                        handler.completed(ex);
                    }
                    subscription.cancel();
                }

                @Override
                public void onComplete() {
                    if (future.complete(null)) {
                        handler.completed(null);
                    }
                }
            };

        }

    }

    /**
     * HTTP资源加载器选项
     */
    public static class Option {

        private long connectTimeoutMs;
        private long timeoutMs;
        private int bufferSize = 102400;

        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }
    }

}
