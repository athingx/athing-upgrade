package io.github.athingx.athing.upgrade.thing.impl.downloader;

import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 下载器
 */
public interface Downloader {

    /**
     * 下载文件
     *
     * @param meta        文件存储元数据
     * @param progress 下载进度
     * @return 下载文件结果
     */
    CompletableFuture<File> download(UpgradeMeta.StoreMeta meta, Progress progress);

    /**
     * 下载进度
     */
    interface Progress {

        /**
         * 下载中
         *
         * @param total      目标下载字节数
         * @param downloaded 已下载字节数
         */
        void progressing(long total, long downloaded);

    }

}
