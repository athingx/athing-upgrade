package io.github.athingx.athing.upgrade.thing;

import io.github.athingx.athing.thing.api.plugin.ThingPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * 设备升级
 */
public interface ThingUpgrade extends ThingPlugin {

    String PLUGIN_ID = "athingx.thing.upgrade";

    /**
     * 通告模块
     *
     * @param module  模块ID
     * @param version 模块版本
     * @return 通告操作
     */
    CompletableFuture<Void> inform(String module, String version);

    /**
     * 主动更新模块
     *
     * @param module 模块ID
     * @return 拉取操作
     */
    CompletableFuture<Void> update(String module);

}
