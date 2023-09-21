package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.upgrade.thing.Upgrade;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * 升级包元数据
 *
 * @param module  模块
 * @param version 版本
 * @param isDiff  是否差分包
 * @param trigger 升级触发源
 * @param extras  附加信息
 * @param stores  存储元数据集合
 */
public record UpgradeMeta(
        String module,
        String version,
        boolean isDiff,
        Upgrade.Trigger trigger,
        Map<String, String> extras,
        Set<StoreMeta> stores
) {

    /**
     * 存储元数据
     *
     * @param name 名称
     * @param uri  URI
     * @param size 大小
     * @param info 信息
     * @param sign 签名
     */
    public record StoreMeta(String name, URI uri, long size, Info info, Sign sign) {

        /**
         * 关联升级包信息
         *
         * @param module  模块
         * @param version 版本
         */
        public record Info(String module, String version) {

        }

        /**
         * 签名
         *
         * @param algorithm 算法
         * @param checksum  校验和
         */
        public record Sign(String algorithm, String checksum) {

        }

    }

}
