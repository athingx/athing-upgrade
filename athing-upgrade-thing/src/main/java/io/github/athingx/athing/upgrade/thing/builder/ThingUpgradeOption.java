package io.github.athingx.athing.upgrade.thing.builder;

public class ThingUpgradeOption {

    /**
     * 下载升级文件连接超时时间
     */
    private long connectTimeoutMs = 1000L * 60;

    /**
     * 下载升级文件超时时间
     */
    private long timeoutMs = 3L * 1000 * 60;

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

}
