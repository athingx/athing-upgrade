package io.github.athingx.athing.upgrade.thing;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 升级包
 */
public record Upgrade(String module, String version, boolean isDiff, Trigger trigger, Map<String, String> extras, Set<Store> stores) {


    /**
     * 触发来源
     */
    public enum Trigger {

        /**
         * 被动推送
         */
        PUSH,

        /**
         * 主动拉取
         */
        PULL

    }

    /**
     * 升级包文件存储
     *
     * @param name        存储名称
     * @param uri         存储URI
     * @param total       存储大小
     * @param persistence 持久化
     */
    public record Store(String name, URI uri, long total, Persistence persistence) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Store store
                    && Objects.equals(name, store.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        /**
         * 持久化
         */
        public interface Persistence {

            /**
             * 持久化存储
             *
             * @param isFlush 是否刷新持久化操作；如果刷新则重新下载并覆盖原文件，否则直接返回原操作结果
             * @return 持久化文件操作
             */
            CompletableFuture<File> persist(boolean isFlush);

        }


    }

}
