package io.github.athingx.athing.upgrade.thing.impl;

/**
 * 升级处理器，用来处理升级过程
 */
public interface UpgradeProcessor {

    /**
     * 进度报告
     *
     * @param step 当前进度
     * @param desc 进度描述
     */
    void processing(int step, String desc);

    /**
     * 过程步骤
     * <pre>
     *     [0 , 50] : 下载升级包
     *     (50, 60] : 校验升级包
     *     (60, 70] : 烧录升级包
     *     (70, 00] : 升级完成
     * </pre>
     */
    interface Step {

        /**
         * 下载完成
         */
        int STEP_DOWNLOAD_COMPLETED = 50;

        /**
         * 校验完成
         */
        int STEP_CHECKSUM_COMPLETED = 60;

        /**
         * 烧录完成
         */
        int STEP_WRITINGS_COMPLETED = 70;

        /**
         * 升级完成
         */
        int STEP_UPGRADES_COMPLETED = 100;

        /**
         * 升级失败
         */
        int STEP_UPGRADES_FAILURE = -1;

        /**
         * 下载失败
         */
        int STEP_DOWNLOAD_FAILURE = -2;

        /**
         * 校验失败
         */
        int STEP_CHECKSUM_FAILURE = -3;

        /**
         * 烧录失败
         */
        int STEP_WRITINGS_FAILURE = -4;

    }
}
