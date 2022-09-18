package io.github.athingx.athing.upgrade.platform.message.decoder;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.common.gson.GsonFactory;
import io.github.athingx.athing.platform.api.message.decoder.DecodeException;
import io.github.athingx.athing.platform.api.message.decoder.ThingMessageDecoder;
import io.github.athingx.athing.upgrade.platform.message.ThingUpgradeProgressMessage;

/**
 * <a href="https://help.aliyun.com/document_detail/73736.html#section-s8w-mik-0m5">OTA升级进度通知</a>
 */
public class ThingUpgradeProgressMessageDecoder implements ThingMessageDecoder<ThingUpgradeProgressMessage> {

    @Override
    public ThingUpgradeProgressMessage[] decode(String jmsMessageId, String jmsMessageTopic, String jmsMessageBody) throws DecodeException {

        if (!jmsMessageTopic.matches("^/[^/]+/[^/]+/ota/progress/post")) {
            return null;
        }

        final Data data = GsonFactory.getGson().fromJson(jmsMessageBody, Data.class);
        return new ThingUpgradeProgressMessage[]{
                new ThingUpgradeProgressMessage(
                        data.productId,
                        data.thingId,
                        data.timestamp,
                        data.moduleId,
                        data.sourceVersion,
                        data.targetVersion,
                        data.step,
                        data.desc
                )
        };
    }

    private record Data(
            @SerializedName("productKey") String productId,
            @SerializedName("deviceName") String thingId,
            @SerializedName("moduleName") String moduleId,
            @SerializedName("messageCreateTime") long timestamp,
            @SerializedName("srcVersion") String sourceVersion,
            @SerializedName("destVersion") String targetVersion,
            @SerializedName("step") int step,
            @SerializedName("desc") String desc
    ) {

    }

}
