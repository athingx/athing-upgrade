package io.github.athingx.athing.upgrade.thing;

import java.io.File;

public class ThingUpgradeOption {

    /**
     * 升级文件存放目录
     */
    private File upgradeDir = new File("./upgrade");

    /**
     * 请求超时时间
     */
    private long timeoutMs = 3000L;

    /**
     * 下载升级文件连接超时时间
     */
    private long downloadConnectTimeoutMs = 1000L * 10;

    /**
     * 下载升级文件超时时间
     */
    private long downloadTimeoutMs = 1000L * 10;

    /**
     * 下载升级文件缓冲区大小
     */
    private int downloadBufferSize = 1 << 17; //131072;

    public File getUpgradeDir() {
        return upgradeDir;
    }

    public void setUpgradeDir(File upgradeDir) {
        this.upgradeDir = upgradeDir;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getDownloadConnectTimeoutMs() {
        return downloadConnectTimeoutMs;
    }

    public void setDownloadConnectTimeoutMs(long downloadConnectTimeoutMs) {
        this.downloadConnectTimeoutMs = downloadConnectTimeoutMs;
    }

    public long getDownloadTimeoutMs() {
        return downloadTimeoutMs;
    }

    public void setDownloadTimeoutMs(long downloadTimeoutMs) {
        this.downloadTimeoutMs = downloadTimeoutMs;
    }

    public int getDownloadBufferSize() {
        return downloadBufferSize;
    }

    public void setDownloadBufferSize(int downloadBufferSize) {
        this.downloadBufferSize = downloadBufferSize;
    }
}
