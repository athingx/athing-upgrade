package io.github.athingx.athing.upgrade.thing.impl.downloader;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.nio.file.StandardOpenOption.READ;

/**
 * 文件资源加载器
 */
public class FileResourceLoader implements ResourceLoader {

    public static final int BUFFER_SIZE = 10240;
    private final File file;
    private final int bufferSize;

    /**
     * 创建文件资源加载器
     *
     * @param file       文件
     * @param bufferSize 缓冲区大小
     */
    public FileResourceLoader(File file, int bufferSize) {
        this.file = file;
        this.bufferSize = bufferSize;
    }

    /**
     * 创建文件资源加载器
     *
     * @param file 文件
     */
    public FileResourceLoader(File file) {
        this(file, BUFFER_SIZE);
    }

    @Override
    public <T> CompletableFuture<T> load(Executor executor, long position, LoadingHandler<T> handler) {
        return CompletableFuture.runAsync(() -> {

                    handler.begin(position);

                    try (final var channel = FileChannel.open(file.toPath(), READ)) {
                        channel.position(position);

                        final var buffer = ByteBuffer.allocate(bufferSize);
                        while (channel.read(buffer) > 0) {
                            buffer.flip();
                            handler.handle(buffer);
                            buffer.flip();
                        }

                    } catch (Throwable ex) {
                        handler.completed(ex);
                        return;
                    }

                    handler.completed(null);

                }, executor)
                .thenCompose(unused -> handler.getTarget());
    }

}
