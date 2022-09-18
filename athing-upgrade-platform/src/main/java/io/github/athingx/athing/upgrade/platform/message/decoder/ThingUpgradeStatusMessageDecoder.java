package io.github.athingx.athing.upgrade.platform.message.decoder;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.common.gson.GsonFactory;
import io.github.athingx.athing.platform.api.message.decoder.DecodeException;
import io.github.athingx.athing.platform.api.message.decoder.ThingMessageDecoder;
import io.github.athingx.athing.upgrade.platform.message.ThingUpgradeStatusMessage;

/**
 * <a href="https://help.aliyun.com/document_detail/73736.html#section-jsp-k24-o8z">OTA升级状态通知</a>
 */
public class ThingUpgradeStatusMessageDecoder implements ThingMessageDecoder<ThingUpgradeStatusMessage> {

    @Override
    public ThingUpgradeStatusMessage[] decode(String jmsMessageId, String jmsMessageTopic, String jmsMessageBody) throws DecodeException {

        if (!jmsMessageTopic.matches("^/[^/]+/[^/]+/ota/upgrade")) {
            return null;
        }

        final Data data = GsonFactory.getGson().fromJson(jmsMessageBody, Data.class);
        return new ThingUpgradeStatusMessage[]{
                new ThingUpgradeStatusMessage(
                        data.productId,
                        data.thingId,
                        data.timestamp,
                        data.moduleId,
                        data.sourceVersion,
                        data.targetVersion,
                        toStatus(data.oriStatus),
                        data.desc
                )
        };
    }

    private ThingUpgradeStatusMessage.Status toStatus(String oriStatus) {
        return switch (oriStatus) {
            case "SUCCEEDED" -> ThingUpgradeStatusMessage.Status.SUCCESS;
            case "FAILED" -> ThingUpgradeStatusMessage.Status.FAILURE;
            case "CANCELED" -> ThingUpgradeStatusMessage.Status.CANCELED;
            default -> null;
        };
    }

    private record Data(
            @SerializedName("productKey") String productId,
            @SerializedName("deviceName") String thingId,
            @SerializedName("moduleName") String moduleId,
            @SerializedName("messageCreateTime") long timestamp,
            @SerializedName("srcVersion") String sourceVersion,
            @SerializedName("destVersion") String targetVersion,
            @SerializedName("status") String oriStatus,
            @SerializedName("desc") String desc
    ) {

    }

}
