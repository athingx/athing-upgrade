package io.github.athingx.athing.upgrade.thing.impl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 文件工具类
 */
public class FileUtils {

    /**
     * 创建文件，并且创建文件所在的目录
     *
     * @param file 文件
     * @throws IOException 创建文件失败
     */
    public static void touchFile(File file) throws IOException {

        final var directory = file.getParentFile();

        // 如果目录不存在则创建目录，创建目录后仍然不存在则抛出异常
        if (!directory.exists() && !directory.mkdirs() || !directory.exists()) {
            throw new IOException("create directory error!");
        }


        // 如果文件不存在则创建文件，创建文件后仍然不存在则抛出异常
        if (!file.exists() && !file.createNewFile() || !file.exists()) {
            throw new IOException("create file error!");
        }

    }

    /**
     * 判断文件是否存在
     *
     * @param file 文件
     * @return TRUE | FALSE
     */
    public static boolean isExistsFile(File file) {
        return file.exists() && file.isFile();
    }

    /**
     * 复制文件
     *
     * @param src  源文件
     * @param dest 目标文件
     * @throws IOException 复制文件失败
     */
    public static void copyFile(File src, File dest) throws IOException {

        try (final FileInputStream fis = new FileInputStream(src);
             final FileOutputStream fos = new FileOutputStream(dest)) {

            final byte[] data = new byte[8192];
            int size;

            while ((size = fis.read(data, 0, data.length)) != -1) {
                fos.write(data, 0, size);
            }
            fos.flush();

        }

    }

    /**
     * 安静地删除一个文件
     *
     * @param target 目标文件
     * @return TRUE | FALSE
     */
    public static boolean deleteQuietly(File target) {
        if (!target.canWrite() || !target.isFile()) {
            return false;
        }
        if (!target.exists()) {
            return true;
        }
        return target.delete();
    }

}
