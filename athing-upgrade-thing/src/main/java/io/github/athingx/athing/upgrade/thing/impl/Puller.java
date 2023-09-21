package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.op.ThingOpBinder;

import java.util.concurrent.CompletableFuture;

/**
 * 模块升级信息拉取器
 */
public interface Puller extends ThingOpBinder {

    /**
     * 拉取模块升级信息
     *
     * @param module 模块
     * @return 拉取操作
     */
    CompletableFuture<UpgradeMeta> pull(String module);

}
