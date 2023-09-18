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

    /**
     * 升级状态
     */
    enum State {

        /**
         * 延后升级
         */
        UPGRADE_LATER,

        /**
         * 升级完成
         */
        UPGRADE_COMPLETED
    }

}
