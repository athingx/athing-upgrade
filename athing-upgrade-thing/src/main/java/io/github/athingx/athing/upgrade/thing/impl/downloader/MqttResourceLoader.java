package io.github.athingx.athing.upgrade.thing.impl.downloader;

import io.github.athingx.athing.upgrade.thing.impl.Digger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * MQTT资源加载器
 */
public class MqttResourceLoader implements ResourceLoader {

    private static final int BUFFER_SIZE = 1 << 17;

    private final Digger digger;
    private final Info info;
    private final Option option;

    /**
     * 创建MQTT资源加载器
     *
     * @param digger MQTT下载器
     * @param option 加载选项
     * @param info   资源信息
     */
    public MqttResourceLoader(Digger digger, Option option, Info info) {
        this.digger = digger;
        this.option = option;
        this.info = info;
    }

    @Override
    public <T> CompletableFuture<T> load(Executor executor, long position, LoadingHandler<T> handler) {
        return CompletableFuture.runAsync(() -> handler.begin(position), executor)
                .thenCompose(unused -> {
                    final var delayed = delayedExecutor(option.getDelayMs(), MILLISECONDS, executor);
                    final var retries = 0;
                    return load(delayed, position, retries, handler)
                            .whenComplete((response, ex) -> handler.completed(ex))
                            .thenCompose(response -> handler.getTarget());
                });
    }

    private Digger.Request toRequest(long position) {
        return new Digger.Request(
                info.streamId(),
                info.fileId(),
                info.total(),
                position,
                (int) Math.min(BUFFER_SIZE, info.total() - position)
        );
    }

    /**
     * 加载数据
     *
     * @param executor 加载执行器
     * @param position 加载起始位置
     * @param retries  重试次数
     * @param handler  加载处理器
     * @param <T>      加载结果类型
     * @return 加载结果
     */
    private <T> CompletableFuture<Digger.Response> load(Executor executor, long position, int retries, LoadingHandler<T> handler) {
        return digger.download(toRequest(position))

                // 加载数据失败则进行重试，直到重试次数耗尽
                .exceptionallyComposeAsync(ex -> {
                    if (retries < option.getRetries()) {
                        return load(executor, position, retries + 1, handler);
                    } else {
                        return failedFuture(ex);
                    }
                })

                // 持续加载数据，直到加载完成
                .thenComposeAsync(response -> {
                    try {

                        final var size = response.buffer().remaining();
                        handler.handle(response.buffer());

                        final var next = response.position() + size;
                        return next < info.total()
                                ? load(executor, next, 0, handler)
                                : completedFuture(response);

                    } catch (Throwable ex) {
                        return failedFuture(ex);
                    }
                }, executor);
    }

    /**
     * 资源信息
     *
     * @param streamId 流ID
     * @param fileId   文件ID
     * @param total    总字节数
     */
    public record Info(String streamId, String fileId, long total) {

    }

    /**
     * 加载选项
     */
    public static class Option {

        private long delayMs = 1000;
        private int retries = 3;

        public long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

    }

}
