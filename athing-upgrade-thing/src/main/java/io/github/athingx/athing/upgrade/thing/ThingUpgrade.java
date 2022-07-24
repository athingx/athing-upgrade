package io.github.athingx.athing.upgrade.thing;

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
     * 更新模块版本
     *
     * @param moduleId 模块ID
     * @param version  模块版本（当前版本）
     * @return 更新Future
     */
    CompletableFuture<Void> update(String moduleId, String version);

}
