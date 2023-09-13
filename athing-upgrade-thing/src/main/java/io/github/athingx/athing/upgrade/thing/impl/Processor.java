package io.github.athingx.athing.upgrade.thing.impl;

import java.util.concurrent.CompletableFuture;

/**
 * 升级处理器，用来处理升级过程
 */
public interface Processor {

    /**
     * 进度报告
     *
     * @param step 当前进度
     * @param desc 进度描述
     */
    CompletableFuture<Void> processing(int step, String desc);

    default CompletableFuture<Void> processing(Throwable cause, int def) {
        return cause instanceof ProcessingException pCause
                ? processing(pCause.getStep(), pCause.getLocalizedMessage())
                : processing(def, cause.getLocalizedMessage());
    }

    /**
     * 过程步骤
     * <pre>
     *     [0 , 70] : 下载升级包
     *     (70, 80] : 校验升级包
     *     (80, 90] : 烧录升级包
     *     (90, 00] : 升级完成
     * </pre>
     */
    interface Step {

        /**
         * 下载完成
         */
        int STEP_DOWNLOAD_COMPLETED = 70;

        /**
         * 校验完成
         */
        int STEP_CHECKSUM_COMPLETED = 80;

        /**
         * 烧录完成
         */
        int STEP_WRITINGS_COMPLETED = 90;

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
