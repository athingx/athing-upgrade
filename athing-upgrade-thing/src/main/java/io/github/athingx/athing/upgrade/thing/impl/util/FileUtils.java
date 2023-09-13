package io.github.athingx.athing.upgrade.thing.impl.util;

import java.io.File;
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

}
