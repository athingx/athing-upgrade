package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.function.CompletableFutureFn;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.builder.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.domain.Meta;
import io.github.athingx.athing.upgrade.thing.impl.util.FileUtils;
import io.github.athingx.athing.upgrade.thing.impl.util.HttpUtils;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.athingx.athing.thing.api.function.CompletableFutureFn.tryCatchExecute;
import static io.github.athingx.athing.upgrade.thing.impl.UpgradeProcessor.Step.*;
import static java.lang.String.format;

public class UpgradeImpl implements Upgrade {

    private final Thing thing;
    private final Meta meta;
    private final ThingUpgradeOption option;
    private final UpgradeProcessor processor;
    private final AtomicReference<CompletableFuture<File>> futureRef = new AtomicReference<>();


    public UpgradeImpl(Thing thing, Meta meta, ThingUpgradeOption option, UpgradeProcessor processor) {
        this.thing = thing;
        this.meta = meta;
        this.option = option;
        this.processor = processor;
    }


    @Override
    public boolean isUpdated() {
        return null != meta;
    }

    private void checkUpdated() {
        if (!isUpdated()) {
            throw new IllegalStateException("not updated!");
        }
    }

    @Override
    public String getModuleId() {
        checkUpdated();
        return meta.moduleId();
    }

    @Override
    public String getVersion() {
        checkUpdated();
        return meta.version();
    }

    @Override
    public long getSize() {
        checkUpdated();
        return meta.size();
    }

    private CompletableFuture<File> initFuture() {
        return tryCatchExecute(new CompletableFutureFn.Executable<>() {

            /**
             * 下载升级包文件
             *
             * @return 升级包文件
             * @throws ModuleUpgradeProcessException 下载失败
             */
            private File download() throws ModuleUpgradeProcessException {
                try {

                    // 创建临时文件
                    final File file = File.createTempFile(
                            String.format("athing-module_%s_%s-",
                                    meta.moduleId(),
                                    meta.upgradeMD5()
                            ),
                            ".ota"
                    );

                    final AtomicInteger currentRef = new AtomicInteger(10);

                    // 下载文件
                    HttpUtils.download(
                            new URL(meta.upgradeURL()),
                            option.getConnectTimeoutMs(),
                            option.getTimeoutMs(),
                            file,
                            process -> {
                                final int step = process / 2;
                                final int current = currentRef.get();
                                if (step >= current) {
                                    processor.processing(current, "downloading...");
                                    currentRef.set(current + 10);
                                }
                            });

                    processor.processing(STEP_DOWNLOAD_COMPLETED, "download completed!");
                    return file;

                } catch (Exception cause) {
                    throw new ModuleUpgradeProcessException(
                            getModuleId(),
                            STEP_DOWNLOAD_FAILURE,
                            cause
                    );
                }
            }

            /**
             * 校验升级包文件
             *
             * @param file 升级包文件
             * @return 升级包文件
             * @throws ModuleUpgradeProcessException 校验失败
             */
            private File checksum(File file) throws ModuleUpgradeProcessException {
                try {
                    final String expect = meta.upgradeCHS().toUpperCase();
                    final String actual = FileUtils.md5(file).toUpperCase();
                    if (!Objects.equals(expect, actual)) {
                        throw new ModuleUpgradeProcessException(
                                getModuleId(),
                                STEP_CHECKSUM_FAILURE,
                                format("checksum failure, expect: %s but actual: %s", expect, actual)
                        );
                    }

                    processor.processing(STEP_CHECKSUM_COMPLETED, "checksum completed!");
                    return file;
                } catch (ModuleUpgradeProcessException cause) {
                    throw cause;
                } catch (Exception cause) {
                    throw new ModuleUpgradeProcessException(
                            getModuleId(),
                            STEP_CHECKSUM_FAILURE,
                            cause
                    );
                }
            }

            @Override
            public void execute(CompletableFuture<File> future) {
                if (!isUpdated()) {
                    future.complete(null);
                    return;
                }
                thing.executor().execute(() -> {
                    try {
                        future.complete(checksum(download()));
                    } catch (Exception cause) {
                        future.completeExceptionally(cause);
                        if (cause instanceof final ModuleUpgradeProcessException mupE) {
                            processor.processing(mupE.getStep(), mupE.getDesc());
                        } else {
                            processor.processing(STEP_DOWNLOAD_FAILURE, cause.getLocalizedMessage());
                        }
                    }
                });
            }


        });
    }

    @Override
    public CompletableFuture<File> getFile() {
        checkUpdated();
        var future = futureRef.get();
        if (future != null) {
            return future;
        }
        synchronized (this) {
            if ((future = futureRef.get()) == null) {
                futureRef.set(future = initFuture());
            }
            return future;
        }
    }

}
