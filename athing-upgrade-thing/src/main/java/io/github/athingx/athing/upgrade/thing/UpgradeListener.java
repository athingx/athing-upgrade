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

    static UpgradeListener persist(UpgradeListener listener) {
        return upgrade -> CompletableFuture.allOf(upgrade.stores().stream()
                        .map(store -> store.persistence().persist(false))
                        .toArray(CompletableFuture[]::new))
                .thenCombine(listener.apply(upgrade), (v, s) -> s);
    }

    static UpgradeListener later() {
        return upgrade -> CompletableFuture.completedFuture(State.UPGRADE_LATER);
    }

}
