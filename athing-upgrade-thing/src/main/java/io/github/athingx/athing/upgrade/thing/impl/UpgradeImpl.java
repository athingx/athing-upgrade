package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.util.CompletableFutureUtils;
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

import static io.github.athingx.athing.thing.api.util.CompletableFutureUtils.tryCatchExecute;
import static io.github.athingx.athing.upgrade.thing.impl.UpgradeProcessor.Step.*;
import static java.lang.String.format;

public class UpgradeImpl implements Upgrade {

    private final Meta meta;
    private final CompletableFuture<File> future;

    public UpgradeImpl(Thing thing, Meta meta, ThingUpgradeOption option, UpgradeProcessor processor) {
        this.meta = meta;
        this.future = tryCatchExecute(new CompletableFutureUtils.Executable<>() {

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
                            String.format("athing-modular_%s_%s-",
                                    meta.moduleId(),
                                    meta.upgradeMD5()
                            ),
                            ".push"
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
    public String getModuleId() {
        return meta.moduleId();
    }

    @Override
    public String getVersion() {
        return meta.version();
    }

    @Override
    public long getSize() {
        return meta.size();
    }

    @Override
    public CompletableFuture<File> getFile() {
        return future;
    }

}
