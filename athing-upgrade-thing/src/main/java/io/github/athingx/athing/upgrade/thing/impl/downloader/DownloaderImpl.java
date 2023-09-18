package io.github.athingx.athing.upgrade.thing.impl.downloader;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.Digger;
import io.github.athingx.athing.upgrade.thing.impl.ProcessingException;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.upgrade.thing.impl.Processor.Step.*;
import static io.github.athingx.athing.upgrade.thing.impl.util.FileUtils.touchFile;
import static io.github.athingx.athing.upgrade.thing.impl.util.StringUtils.isInIgnoreCase;

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
    public CompletableFuture<File> download(UpgradeMeta.StoreMeta sMeta, Downloading downloading) {

        // 目标文件
        final var upgradeFile = toUpgradeFile(sMeta);
        final var sign = sMeta.sign();

        return new ChecksumFileLoader(upgradeFile, sign.type(), sign.code())
                .load(thing.executor(), FileLoader.Loading.empty())
                .exceptionallyCompose(ex -> load(sMeta, upgradeFile, downloading::onDownload))
                .thenCompose(file -> checksum(file, upgradeFile, sign));
    }

    private CompletableFuture<File> load(UpgradeMeta.StoreMeta sMeta, File upgradeFile, FileLoader.Loading loading) {

        // 创建临时文件承接下载数据
        final var downloadingFile = new File(upgradeFile.getParentFile(), upgradeFile.getName() + ".downloading");
        try {
            touchFile(downloadingFile);
        } catch (IOException cause) {
            throw new ProcessingException(STEP_WRITINGS_FAILURE, "create file error!", cause);
        }

        // 根据scheme选择对应的下载方案
        final var scheme = sMeta.uri().getScheme();
        final var isHttp = isInIgnoreCase(scheme, "http", "https");
        final var isMqtt = isInIgnoreCase(scheme, "mqtt");

        // HTTP下载
        if (isHttp) {
            return new HttpFileLoader(
                    thing,
                    option,
                    sMeta.uri(),
                    sMeta.size(),
                    downloadingFile
            ).load(thing.executor(), loading);
        }

        // MQTT下载
        else if (isMqtt) {
            return new MqttFileLoader(
                    thing,
                    digger,
                    sMeta.uri().getHost(),
                    sMeta.uri().getPath().replaceFirst("/", ""),
                    sMeta.size(),
                    downloadingFile
            ).load(thing.executor(), loading);
        }

        // 不支持的下载协议
        else {
            throw new ProcessingException(STEP_UPGRADES_FAILURE, "unsupported scheme: %s".formatted(scheme));
        }
    }

    private CompletableFuture<File> checksum(File file, File upgradeFile, UpgradeMeta.StoreMeta.Sign sign) {
        // 如果是正式文件则直接返回
        if (file == upgradeFile) {
            return CompletableFuture.completedFuture(file);
        }

        // 校验下载文件的签名
        return new ChecksumFileLoader(file, sign.type(), sign.code())
                .load(thing.executor(), FileLoader.Loading.empty())

                // 校验失败则删除下载文件并抛出异常
                .exceptionally(ex -> {
                    // 删除下载文件失败，需要打日志告警：下次下载仍然还会从这个错误的文件继续下载，需要人工介入
                    if (!file.delete()) {
                        logger.warn("{}/upgrade/download delete file failure! file={};", thing.path(), file);
                    }
                    logger.debug(">>> checksum failure! file={};", file, ex);
                    throw new ProcessingException(STEP_CHECKSUM_FAILURE, "checksum failure!");
                })

                // 清理已存在的正式文件，并将临时文件改名为正式文件
                .thenApply(downloadingFile -> {
                    if (upgradeFile.exists() && !upgradeFile.delete() || !downloadingFile.renameTo(upgradeFile)) {
                        throw new ProcessingException(STEP_WRITINGS_FAILURE, "rename file error!");
                    }
                    return upgradeFile;
                });
    }

    private File toUpgradeFile(UpgradeMeta.StoreMeta sMeta) {
        return new File(option.getUpgradeDir(), thing.path().getProductId()
                + File.separator + thing.path().getThingId()
                + File.separator + sMeta.info().module()
                + File.separator + sMeta.info().version()
                + File.separator + sMeta.name()
        );
    }

}
