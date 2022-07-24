package io.github.athingx.athing.upgrade.thing;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 升级包
 */
public interface Upgrade {

    /**
     * 是否有更新
     *
     * @return TRUE | FALSE
     */
    boolean isUpdated();

    /**
     * 获取模块ID
     *
     * @return 模块ID
     */
    String getModuleId();

    /**
     * 获取升级版本
     *
     * @return 升级版本
     */
    String getVersion();

    /**
     * 获取升级文件大小
     *
     * @return 升级文件大小
     */
    long getSize();

    /**
     * 获取升级文件
     *
     * @return 获取文件凭证
     */
    CompletableFuture<File> getFile();

}
