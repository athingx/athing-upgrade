package io.github.athingx.athing.upgrade.thing.impl.downloader;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.ProcessingException;
import io.github.athingx.athing.upgrade.thing.impl.util.ExceptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.STEP_DOWNLOAD_FAILURE;
import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.STEP_WRITINGS_FAILURE;
import static io.github.athingx.athing.upgrade.thing.impl.util.IOUtils.closeQuietly;

record HttpFileLoader(Thing thing, ThingUpgradeOption option, URI uri, long total, File target)
        implements FileLoader {

    @Override
    public CompletableFuture<File> load(Executor executor, Loading loading) {
        final var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(option.getDownloadConnectTimeoutMs()))
                .executor(thing.executor())
                .build();

        final var request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(option.getDownloadTimeoutMs()))
                .header("Range", "bytes=%d-".formatted(target.length()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.buffering(new FileBodyHandler(total, target, loading), option.getDownloadBufferSize()))
                .thenApply(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 206) {
                        throw new ProcessingException(STEP_DOWNLOAD_FAILURE, "http code: %s".formatted(response.statusCode()));
                    }
                    return response.body();
                });
    }

    private record FileBodyHandler(long total, File file, Loading loading) implements HttpResponse.BodyHandler<File> {

        @Override
        public HttpResponse.BodySubscriber<File> apply(HttpResponse.ResponseInfo responseInfo) {
            return new HttpResponse.BodySubscriber<>() {

                private final CompletableFuture<File> future = new CompletableFuture<>();
                private Flow.Subscription subscription;
                private RandomAccessFile raf;
                private FileChannel channel;
                private long position;

                @Override
                public CompletionStage<File> getBody() {
                    return future;
                }

                private static RandomAccessFile openRandomAccessFile(File file) {
                    try {
                        return new RandomAccessFile(file, "rw");
                    } catch (FileNotFoundException e) {
                        throw new ProcessingException(STEP_WRITINGS_FAILURE, "open file error!", e);
                    }
                }

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.raf = openRandomAccessFile(file);
                    this.channel = raf.getChannel();
                    subscription.request(1);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    item.forEach(buffer -> {
                        try {
                            final var remaining = buffer.remaining();
                            while (buffer.hasRemaining()) {
                                if (channel.write(buffer) <= 0) {
                                    throw new IOException("write file error!");
                                }
                            }
                            position += remaining;
                            loading.onLoaded(total, position);
                        } catch (IOException e) {
                            throw new ProcessingException(STEP_WRITINGS_FAILURE, "write file error!", e);
                        }
                    });
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable cause) {
                    future.completeExceptionally(ExceptionUtils.wrapBy(
                            ProcessingException.class,
                            cause,
                            () -> new ProcessingException(STEP_DOWNLOAD_FAILURE, "download error!", cause)));
                    subscription.cancel();
                }

                @Override
                public void onComplete() {
                    closeQuietly(channel, raf);
                    future.complete(file);
                }

            };
        }

    }

}
