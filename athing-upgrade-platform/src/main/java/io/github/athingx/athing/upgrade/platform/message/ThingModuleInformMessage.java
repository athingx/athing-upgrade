package io.github.athingx.athing.upgrade.platform.message;

import io.github.athingx.athing.platform.api.message.ThingMessage;

/**
 * 设备模块通告消息
 */
public class ThingModuleInformMessage extends ThingMessage {

    private final String moduleId;
    private final String version;

    /**
     * 设备模块通告消息
     *
     * @param productId 产品ID
     * @param thingId   设备ID
     * @param timestamp 消息时间戳
     */
    public ThingModuleInformMessage(
            String productId, String thingId, long timestamp,
            String moduleId, String version
    ) {
        super(productId, thingId, timestamp);
        this.moduleId = moduleId;
        this.version = version;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getVersion() {
        return version;
    }

}
