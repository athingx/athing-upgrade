package io.github.athingx.athing.upgrade.thing.impl;

import java.util.concurrent.CompletableFuture;

/**
 * 模块版本通告器
 */
public interface Informer {

    /**
     * 通告模块版本
     *
     * @param module  模块
     * @param version 模块版本
     * @return 通告操作
     */
    CompletableFuture<Void> inform(String module, String version);

}
