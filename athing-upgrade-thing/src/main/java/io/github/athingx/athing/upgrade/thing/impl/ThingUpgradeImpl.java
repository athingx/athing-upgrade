package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.athingx.athing.upgrade.thing.UpgradeListener.State.UPGRADE_COMPLETED;
import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.*;

public class ThingUpgradeImpl implements ThingUpgrade, Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
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
        return informer.inform(module, version);
    }

    @Override
    public CompletableFuture<Void> update(String module) {
        return puller.pull(module)
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

                // 需下载字节数
                final var total = (Long) meta.stores().stream()
                        .map(UpgradeMeta.StoreMeta::size)
                        .mapToLong(Long::longValue)
                        .sum();

                // 已下载字节数
                final var storeDownloadedMap = meta.stores().stream()
                        .collect(Collectors.toMap(
                                UpgradeMeta.StoreMeta::name,
                                sMeta -> new AtomicLong()
                        ));

                // 转换为升级包
                final var upgrade = new Upgrade(
                        meta.module(),
                        meta.version(),
                        meta.isDiff(),
                        meta.trigger(),
                        meta.extras(),
                        meta.stores().stream()
                                .map(sMeta -> toUpgradeStore(sMeta, () -> downloader
                                        .download(meta, sMeta, (sMetaTotal, sMetaDownloaded) -> {

                                            final var storeDownloadedRef = storeDownloadedMap.get(sMeta.name());
                                            while (true) {
                                                final var storeDownloaded = storeDownloadedRef.get();
                                                if (storeDownloadedRef.compareAndSet(storeDownloaded, sMetaDownloaded)) {
                                                    break;
                                                }
                                            }

                                            final var downloaded = storeDownloadedMap.values().stream()
                                                    .mapToLong(AtomicLong::longValue)
                                                    .sum();

                                            final var step = (int) (downloaded * 70 / total);
                                            processor.processing(step, "downloading...");
                                        })
                                        .whenComplete((file, cause) -> {
                                            if (Objects.nonNull(cause)) {
                                                processor.processing(cause, STEP_DOWNLOAD_FAILURE);
                                            }
                                        })))
                                .collect(Collectors.toUnmodifiableSet())
                );


                // 开始通知升级
                try {

                    listener.apply(upgrade)
                            .whenComplete((state, cause) -> {

                                // 升级失败
                                if (Objects.nonNull(cause)) {
                                    processor.processing(cause, STEP_UPGRADES_FAILURE);
                                    logger.debug("{}/upgrade apply failure! module={};version={};",
                                            thing.path(),
                                            meta.module(),
                                            meta.version(),
                                            cause
                                    );
                                }

                                // 升级完成
                                else {

                                    // 升级成功单独处理
                                    if (state == UPGRADE_COMPLETED) {
                                        processor.processing(STEP_UPGRADES_COMPLETED, "upgrade completed!");
                                    }

                                    logger.debug("{}/upgrade apply completed! state={};module={};version={};",
                                            thing.path(),
                                            state,
                                            meta.module(),
                                            meta.version()
                                    );

                                }

                            });

                } catch (Throwable cause) {
                    processor.processing(STEP_UPGRADES_FAILURE, cause.getLocalizedMessage());
                    logger.warn("{}/upgrade apply occur error! module={};version={};",
                            thing.path(),
                            meta.module(),
                            meta.version(),
                            cause
                    );
                }


            }
        } catch (InterruptedException cause) {
            logger.debug("{}/upgrade thread interrupted!", thing.path());
            thread.interrupt();
        }

    }

    private Upgrade.Store toUpgradeStore(UpgradeMeta.StoreMeta sMeta, Supplier<CompletableFuture<File>> supplier) {
        return new Upgrade.Store(sMeta.name(), sMeta.uri(), sMeta.size(), new Upgrade.Store.Persistence() {

            private final AtomicReference<CompletableFuture<File>> persistRef = new AtomicReference<>();

            @Override
            public synchronized CompletableFuture<File> persist(boolean isFlush) {
                if (isFlush || Objects.isNull(persistRef.get())) {
                    Optional.ofNullable(persistRef.getAndSet(supplier.get()))
                            .ifPresent(persist -> persist.cancel(true));
                }
                return persistRef.get();
            }

        });
    }

}
