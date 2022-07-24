package io.github.athingx.athing.upgrade.thing.impl;

import java.util.concurrent.CompletableFuture;

public interface Updater {

    /**
     * 更新模块版本
     *
     * @param moduleId 模块ID
     * @param version  模块版本（当前版本）
     * @return 更新Future
     */
    CompletableFuture<Void> update(String moduleId, String version);

}
