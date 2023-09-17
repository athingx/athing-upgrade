package io.github.athingx.athing.upgrade.thing.impl.downloader;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.impl.Digger;
import io.github.athingx.athing.upgrade.thing.impl.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.*;
import java.util.function.Function;

import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.STEP_DOWNLOAD_FAILURE;
import static io.github.athingx.athing.upgrade.thing.impl.util.ExceptionUtils.isCauseBy;
import static java.util.concurrent.CompletableFuture.delayedExecutor;

record MqttFileLoader(Thing thing, Digger digger, String streamId, String fileId, long total, File target)
        implements FileLoader {

    private static final int DEFAULT_DOWNLOAD_BUFFER_SIZE = 1 << 17;

    @Override
    public CompletableFuture<File> load(Executor executor, Loading loading) {
        return load(
                delayedExecutor(1000, TimeUnit.MILLISECONDS, executor),
                target,
                target.length(),
                loading
        );
    }

    private CompletableFuture<File> load(final Executor executor,
                                         final File target,
                                         final long position,
                                         final Loading loading) {
        final var size = (int) Math.min(DEFAULT_DOWNLOAD_BUFFER_SIZE, total - position);
        return digger.download(new Digger.Request(streamId, fileId, total, position, size))
                .thenComposeAsync(response -> {

                    // 向下载文件中写入数据
                    final var buffer = response.buffer();
                    try (final var raf = new RandomAccessFile(target, "rw");
                         final var channel = raf.getChannel()) {
                        channel.position(response.position());
                        while (buffer.hasRemaining()) {
                            if (channel.write(buffer) <= 0) {
                                throw new IOException("write file error!");
                            }
                        }
                    } catch (IOException e) {
                        throw new ProcessingException(STEP_DOWNLOAD_FAILURE, "download occur error!", e);
                    }

                    // 计算下一步下载的位置
                    final var nextPosition = response.position() + size;

                    // 如果已下载完毕则返回下载文件，否则继续下载
                    loading.onLoaded(total, nextPosition);
                    return nextPosition < total
                            ? MqttFileLoader.this.load(executor, target, nextPosition, loading)
                            : CompletableFuture.completedFuture(target);

                }, executor)
                .exceptionallyComposeAsync(new Function<>() {

                    private final Logger logger = LoggerFactory.getLogger(getClass());

                    @Override
                    public CompletionStage<File> apply(Throwable cause) {

                        // 超时异常进行重试
                        if (isCauseBy(cause, TimeoutException.class)) {
                            logger.debug("{}/upgrade/download timeout, will be retry! streamId={};fileId={};position={};",
                                    thing.path(),
                                    streamId,
                                    fileId,
                                    position
                            );
                            return MqttFileLoader.this.load(executor, target, position, loading);
                        }

                        // 其他异常则对外抛出
                        return CompletableFuture.failedFuture(cause);

                    }
                }, executor);
    }

}
