package io.github.athingx.athing.upgrade.thing.impl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    /**
     * 从指定URL下载文件
     *
     * @param url              URL地址
     * @param connectTimeoutMs 连接超时时间
     * @param timeoutMs        超时时间
     * @param file             目标下载文件
     * @param downloading      下载进度
     * @throws IOException 下载文件出错
     */
    public static void download(URL url, long connectTimeoutMs, long timeoutMs, File file, Downloading downloading) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout((int) (connectTimeoutMs));
        connection.setReadTimeout((int) (timeoutMs));
        connection.connect();

        final int code = connection.getResponseCode();
        if (code != 200) {
            throw new IOException("http response code: " + code);
        }

        try (final FileOutputStream output = new FileOutputStream(file);
             final InputStream input = connection.getInputStream()) {

            final byte[] buffer = new byte[2048];
            final int total = connection.getContentLength();
            int size, sum = 0;
            int current = 0;
            while ((size = input.read(buffer)) != -1) {
                sum += size;
                output.write(buffer, 0, size);
                if (null != downloading) {
                    final int process = (int) (sum * 1.0f / total * 100);
                    if (process >= current) {
                        if (current > 0) {
                            downloading.processing(current);
                        }
                        current += 10;
                    }
                }
            }
            output.flush();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 下载进度
     */
    public interface Downloading {

        /**
         * 报告下载进度(0~100)
         *
         * @param process 进度
         */
        void processing(int process);

    }

}
