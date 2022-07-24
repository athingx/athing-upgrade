package io.github.athingx.athing.upgrade.thing.impl;

import java.util.concurrent.CompletableFuture;

/**
 * 通告者
 */
public interface Informer {

    /**
     * 通告
     *
     * @param moduleId 模块ID
     * @param version  模块版本
     * @return 通告操作
     */
    CompletableFuture<Void> inform(String moduleId, String version);

}
