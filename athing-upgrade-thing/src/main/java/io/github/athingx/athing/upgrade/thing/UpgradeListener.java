package io.github.athingx.athing.upgrade.thing;

import java.util.concurrent.CompletableFuture;

/**
 * 升级监听器
 */
public interface UpgradeListener {

    /**
     * 升级
     *
     * @param upgrade 升级包
     * @return 升级结果
     */
    CompletableFuture<State> apply(Upgrade upgrade);

    enum State {
        UPGRADE_LATER,
        UPGRADE_COMPLETED
    }

}
