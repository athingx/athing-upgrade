package io.github.athingx.athing.upgrade.thing.impl.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.github.athingx.athing.upgrade.thing.impl.util.IOUtils.closeQuietly;
import static io.github.athingx.athing.upgrade.thing.impl.util.StringUtils.bytesToHexString;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * 资源加载器
 */
public interface ResourceLoader {

    /**
     * 加载资源
     *
     * @param executor 加载执行器
     * @param handler  加载处理器
     * @param <T>      资源类型
     * @return 资源加载结果
     */
    default <T> CompletableFuture<T> load(Executor executor, LoadingHandler<T> handler) {
        return load(executor, 0L, handler);
    }

    /**
     * 加载资源
     *
     * @param executor 加载执行器
     * @param position 加载起始位置
     * @param handler  加载处理器
     * @param <T>      资源类型
     * @return 资源加载结果
     */
    <T> CompletableFuture<T> load(Executor executor, long position, LoadingHandler<T> handler);

    /**
     * 加载处理器
     *
     * @param <T> 资源类型
     */
    interface LoadingHandler<T> {

        /**
         * 获取目标资源
         *
         * @return 目标资源
         */
        CompletableFuture<T> getTarget();

        /**
         * 开始加载
         *
         * @param position 加载起始位置
         */
        void begin(long position);

        /**
         * 处理数据
         *
         * @param buffer 数据缓冲区
         * @throws IOException 处理数据失败
         */
        void handle(ByteBuffer buffer) throws IOException;

        /**
         * 加载完成
         *
         * @param ex 加载异常
         */
        void completed(Throwable ex);

    }

    /**
     * 加载处理器工具
     */
    class LoadingHandlers {

        private LoadingHandlers() {

        }

        /**
         * 将数据加载到文件
         *
         * @param file 文件
         * @return 加载处理器
         */
        public static LoadingHandler<File> toFile(File file) {
            return new LoadingHandler<>() {

                private final CompletableFuture<File> future = new CompletableFuture<>();
                private volatile FileChannel channel;

                @Override
                public CompletableFuture<File> getTarget() {
                    return future;
                }

                @Override
                public void begin(long position) {
                    try {
                        channel = FileChannel.open(file.toPath(), CREATE, WRITE);
                        if (position > 0) {
                            channel.position(position);
                        }
                    } catch (IOException cause) {
                        closeQuietly(channel);
                        throw new RuntimeException("open error!", cause);
                    }
                }

                @Override
                public void handle(ByteBuffer buffer) throws IOException {
                    while (buffer.hasRemaining()) {
                        if (channel.write(buffer) <= 0) {
                            throw new IOException("write failed");
                        }
                    }
                }

                @Override
                public void completed(Throwable ex) {
                    closeQuietly(channel);
                    Optional.ofNullable(ex)
                            .ifPresentOrElse(
                                    future::completeExceptionally,
                                    () -> future.complete(file)
                            );
                }

            };
        }


        /**
         * 计算校验和
         *
         * @param algorithm 算法
         * @return 加载处理器
         */
        public static LoadingHandler<byte[]> checksum(String algorithm) {

            return new LoadingHandler<>() {

                private final CompletableFuture<byte[]> future = new CompletableFuture<>();
                private volatile MessageDigest digest;

                @Override
                public CompletableFuture<byte[]> getTarget() {
                    return future;
                }

                @Override
                public void begin(long position) {
                    try {
                        digest = MessageDigest.getInstance(algorithm);
                    } catch (NoSuchAlgorithmException cause) {
                        throw new RuntimeException(cause);
                    }
                }

                @Override
                public void handle(ByteBuffer buffer) {
                    digest.update(buffer);
                }

                @Override
                public void completed(Throwable ex) {
                    Optional.ofNullable(ex)
                            .ifPresentOrElse(
                                    future::completeExceptionally,
                                    () -> future.complete(digest.digest())
                            );
                }

            };
        }


        /**
         * 校验校验和
         *
         * @param algorithm 算法
         * @param checksum  校验和
         * @return 加载处理器
         */
        public static LoadingHandler<Void> verify(String algorithm, String checksum) {
            final var handler = checksum(algorithm);
            return new LoadingHandler<>() {

                @Override
                public CompletableFuture<Void> getTarget() {
                    return handler.getTarget()
                            .thenAccept(bytes -> {
                                if (!checksum.equalsIgnoreCase(bytesToHexString(bytes))) {
                                    throw new RuntimeException("checksum error");
                                }
                            });
                }

                @Override
                public void begin(long position) {
                    handler.begin(position);
                }

                @Override
                public void handle(ByteBuffer buffer) throws IOException {
                    handler.handle(buffer);
                }

                @Override
                public void completed(Throwable ex) {
                    handler.completed(ex);
                }

            };
        }

    }


}
