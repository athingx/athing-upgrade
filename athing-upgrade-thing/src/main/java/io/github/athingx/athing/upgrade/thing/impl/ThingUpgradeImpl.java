package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import io.github.athingx.athing.upgrade.thing.impl.downloader.Downloader;
import io.github.athingx.athing.upgrade.thing.impl.downloader.DownloaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ThingUpgradeImpl implements ThingUpgrade, Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final ThingUpgradeOption option;
    private final Digger digger;
    private final Puller puller;
    private final Pusher pusher;
    private final BlockingQueue<UpgradeMeta> queue;
    private final UpgradeListener listener;
    private final Informer informer;
    private final Downloader downloader;
    private final AtomicReference<Thread> threadRef = new AtomicReference<>();
    private volatile boolean isRunning = true;

    public ThingUpgradeImpl(final Thing thing,
                            final ThingUpgradeOption option,
                            final Digger digger,
                            final Puller puller,
                            final Pusher pusher,
                            final BlockingQueue<UpgradeMeta> queue,
                            final UpgradeListener listener) {
        this.thing = thing;
        this.queue = queue;
        this.option = option;
        this.digger = digger;
        this.puller = puller;
        this.pusher = pusher;
        this.listener = listener;
        this.informer = new InformerImpl(thing);
        this.downloader = new DownloaderImpl(thing, option, digger);

        // 启动升级线程
        thing.executor().execute(this);

    }

    @Override
    public CompletableFuture<Void> inform(String module, String version) {
        return informer
                .inform(module, version)
                .orTimeout(option.getTimeoutMs(), MILLISECONDS)
                ;
    }

    @Override
    public CompletableFuture<Void> update(String module) {
        return puller.pull(module)
                .orTimeout(option.getTimeoutMs(), MILLISECONDS)
                .thenAccept(meta -> {
                    if (Objects.isNull(meta)) {
                        logger.debug("{}/upgrade not essential! module={};", thing.path(), module);
                        return;
                    }
                    if (!queue.offer(meta)) {
                        throw new IllegalStateException("queue overflow!");
                    }
                });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.allOf(digger.unbind(), puller.unbind(), pusher.unbind())
                .thenAccept(unused -> {

                    // 标记升级线程退出
                    isRunning = false;

                    // 通知升级线程退出
                    Optional.ofNullable(threadRef.getAndSet(null)).ifPresent(Thread::interrupt);

                });
    }

    @Override
    public void run() {

        // 当前线程
        final var thread = Thread.currentThread();

        // 检查线程是否已经退出
        if (!isRunning) {
            throw new IllegalStateException("not running!");
        }

        // 注册升级线程
        if (!threadRef.compareAndSet(null, thread)) {
            throw new IllegalStateException("already running!");
        }

        try {
            while (isRunning) {

                final var meta = queue.take();

                // 本次升级进度报告器
                final var processor = new ProcessorImpl(thing, meta.module());

                // 转换为升级包
                final var upgrade = new Upgrade(
                        meta.module(),
                        meta.version(),
                        meta.isDiff(),
                        meta.trigger(),
                        meta.extras(),
                        toUpgradeStoreSet(meta, processor),
                        toApplication(meta, processor)
                );

                // 开始通知升级
                try {
                    listener.apply(upgrade);
                } catch (Throwable cause) {
                    upgrade.application().error(cause);
                }

            }
        } catch (InterruptedException cause) {
            logger.debug("{}/upgrade thread interrupted!", thing.path());
            thread.interrupt();
        }

    }

    private Upgrade.Store toUpgradeStore(UpgradeMeta.StoreMeta sMeta, Supplier<CompletableFuture<File>> supplier) {
        return new Upgrade.Store(
                sMeta.name(),
                sMeta.uri(),
                sMeta.size(),
                toUpgradeStorePersistence(supplier)
        );
    }

    private Upgrade.Store.Persistence toUpgradeStorePersistence(Supplier<CompletableFuture<File>> supplier) {
        return new Upgrade.Store.Persistence() {

            private final AtomicReference<CompletableFuture<File>> futureRef = new AtomicReference<>();

            @Override
            public synchronized CompletableFuture<File> persist(boolean force) {

                // 强制获取
                if (force) {
                    final var future = supplier.get();
                    futureRef.set(future);
                    return future;
                }

                // 优先从之前的获取
                return Optional.ofNullable(futureRef.get())
                        .orElseGet(() -> persist(true));
            }

        };
    }

    private Set<Upgrade.Store> toUpgradeStoreSet(UpgradeMeta meta, Processor processor) {

        // 需下载字节数
        final var total = meta.stores().stream()
                .map(UpgradeMeta.StoreMeta::size)
                .mapToLong(Long::longValue)
                .sum();

        // 已下载字节数
        final var storeDownloadedMap = meta.stores().stream()
                .collect(Collectors.toMap(
                        UpgradeMeta.StoreMeta::name,
                        sMeta -> new AtomicLong()
                ));

        // 生成Store集合
        return meta.stores().stream()
                .map(sMeta -> toUpgradeStore(sMeta, () -> downloader
                        .download(sMeta, (sMetaTotal, sMetaDownloaded) -> {

                            // 记录单模块下载字节数
                            final var storeDownloadedRef = storeDownloadedMap.get(sMeta.name());
                            while (true) {
                                final var storeDownloaded = storeDownloadedRef.get();
                                if (storeDownloadedRef.compareAndSet(storeDownloaded, sMetaDownloaded)) {
                                    break;
                                }
                            }

                            // 计算所有模块下载字节数
                            final var downloaded = storeDownloadedMap.values().stream()
                                    .mapToLong(AtomicLong::longValue)
                                    .sum();

                            // 计算并报告下载进度
                            final var step = (int) (downloaded * STEP_DOWNLOAD_COMPLETED / total);
                            if (step == STEP_DOWNLOAD_COMPLETED) {
                                processor.processing(STEP_DOWNLOAD_COMPLETED, "download completed!");
                            } else {
                                processor.processing(step, "downloading...");
                            }

                        })
                        .whenComplete((file, cause) -> {
                            if (Objects.nonNull(cause)) {
                                processor.processing(cause, STEP_DOWNLOAD_FAILURE);
                            }
                        })))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Upgrade.Application toApplication(UpgradeMeta meta, Processor processor) {
        return new Upgrade.Application() {

            private final CompletableFuture<Void> finishF = new CompletableFuture<>();

            @Override
            public void apply() {
                if (finishF.complete(null)) {
                    processor.processing(STEP_UPGRADES_COMPLETED, "upgrade completed!").join();
                    informer.inform(meta.module(), meta.version()).join();
                    logger.debug("{}/upgrade apply completed! module={};version={};",
                            thing.path(),
                            meta.module(),
                            meta.version()
                    );
                } else {
                    throw new IllegalStateException("already finished!");
                }
            }

            @Override
            public void error() {
                error(new ProcessingException(STEP_UPGRADES_FAILURE, "upgrade failure!"));
            }

            @Override
            public void error(Throwable cause) {
                if (finishF.completeExceptionally(cause)) {
                    processor.processing(cause, STEP_UPGRADES_FAILURE).join();
                    logger.debug("{}/upgrade apply failure! module={};version={};",
                            thing.path(),
                            meta.module(),
                            meta.version(),
                            cause
                    );
                } else {
                    throw new IllegalStateException("already finished!");
                }
            }

        };
    }

}
