package io.github.athingx.athing.upgrade.thing.impl;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 下载器
 */
public interface Downloader {

    /**
     * 下载文件
     *
     * @param sMeta       文件存储元数据
     * @param downloading 下载进度
     * @return 下载文件结果
     */
    CompletableFuture<File> download(UpgradeMeta.StoreMeta sMeta, Downloading downloading);

    /**
     * 下载进度
     */
    interface Downloading {

        /**
         * 下载中
         *
         * @param total      目标下载字节数
         * @param downloaded 已下载字节数
         */
        void onDownload(long total, long downloaded);

    }

}
