package io.github.athingx.athing.upgrade.platform.message;

import io.github.athingx.athing.platform.api.message.ThingMessage;

/**
 * 设备模块升级进度消息
 */
public class ThingUpgradeProgressMessage extends ThingMessage {

    private final String moduleId;
    private final String sourceVersion;
    private final String targetVersion;
    private final int step;
    private final String desc;

    /**
     * 设备消息
     *
     * @param productId 产品ID
     * @param thingId   设备ID
     * @param timestamp 消息时间戳
     */
    public ThingUpgradeProgressMessage(
            String productId, String thingId, long timestamp,
            String moduleId, String sourceVersion, String targetVersion, int step, String desc
    ) {
        super(productId, thingId, timestamp);
        this.moduleId = moduleId;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        this.step = step;
        this.desc = desc;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public int getStep() {
        return step;
    }

    public String getDesc() {
        return desc;
    }

}
