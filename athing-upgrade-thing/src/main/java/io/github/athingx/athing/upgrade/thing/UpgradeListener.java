package io.github.athingx.athing.upgrade.thing;

/**
 * 升级监听器
 */
public interface UpgradeListener {

    /**
     * 升级
     *
     * @param upgrade 升级包
     */
    void apply(Upgrade upgrade);

}
