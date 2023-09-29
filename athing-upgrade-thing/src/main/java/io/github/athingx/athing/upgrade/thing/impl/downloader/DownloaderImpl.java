package io.github.athingx.athing.upgrade.thing.impl.downloader;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.Digger;
import io.github.athingx.athing.upgrade.thing.impl.ProcessingException;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;
import io.github.athingx.athing.upgrade.thing.impl.downloader.ResourceLoader.LoadingHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.*;
import static io.github.athingx.athing.upgrade.thing.impl.util.FileUtils.touchFile;
import static io.github.athingx.athing.upgrade.thing.impl.util.StringUtils.isInIgnoreCase;

/**
 * 下载器实现
 */
public class DownloaderImpl implements Downloader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final ThingUpgradeOption option;
    private final Digger digger;

    public DownloaderImpl(Thing thing, ThingUpgradeOption option, Digger digger) {
        this.thing = thing;
        this.option = option;
        this.digger = digger;
    }

    @Override
    public CompletableFuture<File> download(UpgradeMeta.StoreMeta meta, Progress progress) {

        final var sign = meta.sign();
        final var executor = thing.executor();
        final var upgradeFile = toUpgradeFile(meta);

        return new FileResourceLoader(upgradeFile)
                .load(executor, LoadingHandlers.verify(sign.algorithm(), sign.checksum()))
                .thenApply(unused -> upgradeFile)
                .exceptionallyCompose(ex -> load(meta, upgradeFile, progress));
    }

    /**
     * 加载文件资源
     *
     * @param meta     存储元数据
     * @param target   目标文件
     * @param progress 下载进度
     * @return 加载结果
     */
    private CompletableFuture<File> load(UpgradeMeta.StoreMeta meta, File target, Progress progress) {

        // 创建临时文件承接下载数据
        final var downloadingFile = new File(target.getParentFile(), target.getName() + ".downloading");
        try {
            touchFile(downloadingFile);
        } catch (IOException cause) {
            throw new ProcessingException(STEP_WRITINGS_FAILURE, "create file error!", cause);
        }

        final var executor = thing.executor();
        final var position = downloadingFile.length();
        final var sign = meta.sign();
        final var size = meta.size();

        // 下载文件
        return switchLoader(meta).load(executor, position, new FileProgressHandler(downloadingFile, size, progress))
                .thenCompose(file -> new FileResourceLoader(file)
                        .load(executor, LoadingHandlers.verify(sign.algorithm(), sign.checksum()))

                        // 校验失败则删除下载文件并抛出异常
                        .exceptionally(ex -> {
                            // 删除下载文件失败，需要打日志告警：下次下载仍然还会从这个错误的文件继续下载，需要人工介入
                            if (!file.delete()) {
                                logger.warn("{}/upgrade/download delete file failure! file={};", thing.path(), file);
                            }
                            throw new ProcessingException(STEP_CHECKSUM_FAILURE, "checksum failure!", ex);
                        })

                        // 清理已存在的正式文件，并将临时文件改名为正式文件
                        .thenApply(unused -> {
                            if (target.exists() && !target.delete() || !file.renameTo(target)) {
                                throw new ProcessingException(STEP_WRITINGS_FAILURE, "rename failure!");
                            }
                            return target;
                        }));
    }

    /**
     * 选择资源加载器
     *
     * @param meta 存储元数据
     * @return 资源加载器
     */
    private ResourceLoader switchLoader(UpgradeMeta.StoreMeta meta) {

        // 根据scheme选择对应的下载方案
        final var scheme = meta.uri().getScheme();
        final var isHttp = isInIgnoreCase(scheme, "http", "https");
        final var isMqtt = isInIgnoreCase(scheme, "mqtt");

        // HTTP下载
        if (isHttp) {
            return new HttpResourceLoader(
                    new HttpResourceLoader.Option() {{
                        setConnectTimeoutMs(option.getDownloadConnectTimeoutMs());
                        setTimeoutMs(option.getDownloadTimeoutMs());
                        setBufferSize(option.getDownloadBufferSize());
                    }},
                    meta.uri()
            );
        }

        // MQTT下载
        else if (isMqtt) {
            return new MqttResourceLoader(
                    digger,
                    new MqttResourceLoader.Option() {{
                        setTimeoutMs(option.getTimeoutMs());
                        setDelayMs(1000L);
                        setRetries(3);
                    }},
                    new MqttResourceLoader.Info(
                            meta.uri().getHost(),
                            meta.uri().getPath().replaceFirst("/", ""),
                            meta.size()
                    )
            );
        }

        // 不支持的下载协议
        else {
            throw new ProcessingException(STEP_UPGRADES_FAILURE, "unsupported scheme: %s".formatted(scheme));
        }

    }

    private File toUpgradeFile(UpgradeMeta.StoreMeta sMeta) {
        return new File(option.getUpgradeDir(), thing.path().getProductId()
                + File.separator + thing.path().getThingId()
                + File.separator + sMeta.info().module()
                + File.separator + sMeta.info().version()
                + File.separator + sMeta.name()
        );
    }


    /**
     * 文件进度处理器
     */
    private static class FileProgressHandler implements ResourceLoader.LoadingHandler<File> {

        private final long total;
        private final Progress progress;
        private final ResourceLoader.LoadingHandler<File> handler;
        private final AtomicLong downloadedRef = new AtomicLong();

        /**
         * 文件进度处理器
         *
         * @param target   目标文件
         * @param total    总字节数
         * @param progress 进度
         */
        private FileProgressHandler(File target, long total, Progress progress) {
            this.total = total;
            this.progress = progress;
            this.handler = LoadingHandlers.toFile(target);
        }

        @Override
        public CompletableFuture<File> getTarget() {
            return handler.getTarget();
        }

        @Override
        public void begin(long position) {
            handler.begin(position);
            downloadedRef.set(position);
        }

        @Override
        public void handle(ByteBuffer buffer) throws IOException {
            final var size = buffer.remaining();
            handler.handle(buffer);
            progress.progressing(total, downloadedRef.addAndGet(size));
        }

        @Override
        public void completed(Throwable ex) {
            handler.completed(ex);
        }

    }

}
