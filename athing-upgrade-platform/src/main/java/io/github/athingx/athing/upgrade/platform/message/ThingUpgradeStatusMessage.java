package io.github.athingx.athing.upgrade.platform.message;

import io.github.athingx.athing.platform.api.message.ThingMessage;

/**
 * 设备模块升级结果消息
 */
public class ThingUpgradeStatusMessage extends ThingMessage {

    private final String moduleId;
    private final String sourceVersion;
    private final String targetVersion;
    private final Status status;
    private final String desc;

    public ThingUpgradeStatusMessage(
            String productId, String thingId, long timestamp,
            String moduleId, String sourceVersion, String targetVersion, Status status, String desc
    ) {
        super(productId, thingId, timestamp);
        this.moduleId = moduleId;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        this.status = status;
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

    public Status getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        CANCELED
    }

}
