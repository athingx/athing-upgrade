package io.github.athingx.athing.upgrade.thing.impl.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.github.athingx.athing.upgrade.thing.impl.util.StringUtils.bytesToHexString;

/**
 * 文件校验加载器
 *
 * @param target    目标文件
 * @param algorithm 校验算法
 * @param checksum  校验和
 */
record ChecksumFileLoader(File target, String algorithm, String checksum) implements FileLoader {

    @Override
    public CompletableFuture<File> load(Executor executor, Loading loading) {
        return CompletableFuture.supplyAsync(() -> {

            // 检查文件是否存在，文件不存在校验失败
            if (!target.exists()) {
                throw new ChecksumException("file not exists");
            }

            // 读取文件内容计算校验和
            try (final var fis = new FileInputStream(target)) {
                final var total = target.length();
                final var digest = MessageDigest.getInstance(algorithm);
                final var buffer = new byte[10240];
                int position = 0, len;
                while ((len = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, len);
                    loading.onLoaded(total, position += len);
                }
                if (!checksum.equalsIgnoreCase(bytesToHexString(digest.digest()))) {
                    throw new ChecksumException("checksum error");
                }
                return target;
            }

            // 校验失败异常则抛出校验失败异常
            catch (ChecksumException cause) {
                throw cause;
            }

            // 其他异常则抛出校验失败异常
            catch (Exception cause) {
                throw new ChecksumException("checksum error", cause);
            }

        }, executor);
    }

    /**
     * 校验异常
     */
    public static class ChecksumException extends RuntimeException {

        public ChecksumException(String message) {
            super(message);
        }

        public ChecksumException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
