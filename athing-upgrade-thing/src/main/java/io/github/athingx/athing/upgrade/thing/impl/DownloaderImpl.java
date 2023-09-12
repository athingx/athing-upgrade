package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;

import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.*;

public class DownloaderImpl implements Downloader {

    private final Thing thing;
    private final ThingUpgradeOption option;
    private final Digger digger;

    public DownloaderImpl(Thing thing, ThingUpgradeOption option, Digger digger) {
        this.thing = thing;
        this.option = option;
        this.digger = digger;
    }

    @Override
    public CompletableFuture<File> download(UpgradeMeta meta, UpgradeMeta.StoreMeta sMeta, Downloading downloading) {

        // 检查目标文件是否已经存在，如果已存在则直接返回
        final var upgradeDataFile = toUpgradeFile(meta, sMeta);
        if (FileUtils.isExistsFile(upgradeDataFile)) {
            return CompletableFuture.completedFuture(upgradeDataFile);
        }

        // 创建临时文件承接下载数据
        final var upgradeTempFile = new File(upgradeDataFile.getParentFile(), upgradeDataFile.getName() + ".downloading");
        try {
            FileUtils.touchFile(upgradeTempFile);
        } catch (IOException cause) {
            return CompletableFuture.failedFuture(new ProcessingException(STEP_WRITINGS_FAILURE, "create file error!", cause));
        }

        // 根据scheme选择对应的下载方案
        final var scheme = sMeta.uri().getScheme();
        final var isHttp = StringUtils.isInIgnoreCase(scheme, "http", "https");
        final var isMqtt = StringUtils.isInIgnoreCase(scheme, "mqtt");
        final CompletableFuture<File> future;
        if (isHttp) {
            future = downloadByHttp(sMeta, upgradeTempFile, downloading);
        } else if (isMqtt) {
            future = downloadByMqtt(sMeta, upgradeTempFile, 0L, downloading);
        } else {
            future = CompletableFuture.failedFuture(new ProcessingException(STEP_UPGRADES_FAILURE, "unsupported scheme: %s".formatted(scheme)));
        }

        // 下载完成后需要将临时文件转为正式文件
        return future.thenApply(file -> {

            try {
                final var sign = sMeta.sign();
                final var except = sign.code();
                final var actual = SignUtils.digest(sign.type(), file);
                if (!StringUtils.equalsIgnoreCase(except, actual)) {
                    throw new ProcessingException(STEP_CHECKSUM_FAILURE, "checksum failure!");
                }
            } catch (Exception cause) {
                throw ExceptionUtils.wrapBy(ProcessingException.class, cause, () -> new ProcessingException(STEP_CHECKSUM_FAILURE, "checksum occur error!", cause));
            }

            if (!file.renameTo(upgradeDataFile)) {
                throw new ProcessingException(STEP_WRITINGS_FAILURE, "rename file error!");
            }
            return upgradeDataFile;
        });
    }

    private File toUpgradeFile(UpgradeMeta meta, UpgradeMeta.StoreMeta sMeta) {
        return new File(option.getUpgradeDir(), thing.path().getProductId()
                + File.separator + thing.path().getThingId()
                + File.separator + meta.module()
                + File.separator + meta.version()
                + File.separator + sMeta.name()
        );
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CompletableFuture<File> downloadByMqtt(final UpgradeMeta.StoreMeta sMeta,
                                                   final File upgradeTempFile,
                                                   final long position,
                                                   final Downloading downloading) {
        final var streamId = sMeta.uri().getHost();
        final var fileId = sMeta.uri().getPath().replaceFirst("/", "");
        final var total = sMeta.size();
        final var size = (int) Math.min(option.getDownloadBufferSize(), total - position);

        return digger.download(new Digger.Request(streamId, fileId, total, position, size))
                .thenCompose(response -> {

                    final var buffer = response.buffer();
                    try (final var raf = new RandomAccessFile(upgradeTempFile, "rw");
                         final var channel = raf.getChannel()) {
                        channel.position(response.position());
                        while (buffer.hasRemaining()) {
                            if (channel.write(buffer) <= 0) {
                                throw new IOException("write file error!");
                            }
                        }
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(new ProcessingException(STEP_DOWNLOAD_FAILURE, "download occur error!", e));
                    }

                    final var nextPosition = response.position() + size;
                    downloading.onDownload(total, nextPosition);
                    return nextPosition < total
                            ? downloadByMqtt(sMeta, upgradeTempFile, nextPosition, downloading)
                            : CompletableFuture.completedFuture(upgradeTempFile);

                })
                .exceptionallyCompose(cause -> {
                    // 超时异常进行重试
                    if (ExceptionUtils.isCauseBy(cause, TimeoutException.class)) {
                        logger.debug("{}/upgrade/download timeout, will be retry! streamId={};fileId={};position={};size={};",
                                thing.path(),
                                streamId,
                                fileId,
                                position,
                                size
                        );
                        return downloadByMqtt(sMeta, upgradeTempFile, position, downloading);
                    }

                    // 其他异常则对外抛出
                    else {
                        return CompletableFuture.failedFuture(cause);
                    }
                })
                ;
    }

    private CompletableFuture<File> downloadByHttp(final UpgradeMeta.StoreMeta sMeta,
                                                   final File upgradeTempFile,
                                                   final Downloading downloading) {

        final var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(option.getDownloadConnectTimeoutMs()))
                .executor(thing.executor())
                .build();

        final var request = HttpRequest.newBuilder(sMeta.uri())
                .timeout(Duration.ofMillis(option.getDownloadTimeoutMs()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.buffering(new FileBodyHandler(upgradeTempFile, sMeta, downloading), option.getDownloadBufferSize()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new ProcessingException(STEP_DOWNLOAD_FAILURE, "http code: %s".formatted(response.statusCode()));
                    }
                    return response.body();
                });
    }

    private record FileBodyHandler(
            File file,
            UpgradeMeta.StoreMeta sMeta,
            Downloading downloading) implements HttpResponse.BodyHandler<File> {

        @Override
        public HttpResponse.BodySubscriber<File> apply(HttpResponse.ResponseInfo responseInfo) {
            return new HttpResponse.BodySubscriber<>() {

                private final CompletableFuture<File> future = new CompletableFuture<>();
                private Flow.Subscription subscription;
                private RandomAccessFile raf;
                private FileChannel channel;
                private long downloaded;

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
                            downloaded += remaining;
                            downloading.onDownload(sMeta.size(), downloaded);
                        } catch (IOException e) {
                            throw new ProcessingException(STEP_WRITINGS_FAILURE, "write file error!", e);
                        }
                    });
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable ex) {
                    future.completeExceptionally(ex instanceof ProcessingException cause
                            ? cause
                            : new ProcessingException(STEP_DOWNLOAD_FAILURE, "download error!", ex));
                    subscription.cancel();
                }

                @Override
                public void onComplete() {
                    IOUtils.closeQuietly(channel, raf);
                    future.complete(file);
                }

            };
        }

    }
}