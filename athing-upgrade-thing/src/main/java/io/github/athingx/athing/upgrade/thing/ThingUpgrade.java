package io.github.athingx.athing.upgrade.thing;

import io.github.athingx.athing.thing.api.op.OpReply;

import java.util.concurrent.CompletableFuture;

/**
 * 设备升级
 */
public interface ThingUpgrade {

    /**
     * 添加模块升级监听器
     *
     * @param listener 监听器
     */
    void appendListener(UpgradeListener listener);

    /**
     * 移除模块升级监听器
     *
     * @param listener 模块升级监听器
     */
    void removeListener(UpgradeListener listener);

    /**
     * 通告模块
     *
     * @param moduleId 模块ID
     * @param version  模块版本
     * @return 通告操作
     */
    CompletableFuture<Void> inform(String moduleId, String version);

    /**
     * 更新模块最新版本
     *
     * @param moduleId 模块ID
     * @return 更新操作
     */
    CompletableFuture<Void> update(String moduleId);

    /**
     * 拉取模块更新
     *
     * @param moduleId 模块ID
     * @return 拉取操作
     */
    CompletableFuture<OpReply<Upgrade>> fetch(String moduleId);

}
