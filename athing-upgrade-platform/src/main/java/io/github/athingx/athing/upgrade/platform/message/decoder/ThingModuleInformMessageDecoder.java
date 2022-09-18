package io.github.athingx.athing.upgrade.platform.message.decoder;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.common.gson.GsonFactory;
import io.github.athingx.athing.platform.api.message.decoder.DecodeException;
import io.github.athingx.athing.platform.api.message.decoder.ThingMessageDecoder;
import io.github.athingx.athing.upgrade.platform.message.ThingModuleInformMessage;

/**
 * <a href="https://help.aliyun.com/document_detail/73736.html#section-g60-o9o-glb">OTA模块版本号变更通知</a>
 */
public class ThingModuleInformMessageDecoder implements ThingMessageDecoder<ThingModuleInformMessage> {

    @Override
    public ThingModuleInformMessage[] decode(String jmsMessageId, String jmsMessageTopic, String jmsMessageBody) throws DecodeException {

        if (!jmsMessageTopic.matches("^/[^/]+/[^/]+/ota/version/post")) {
            return null;
        }

        final Data data = GsonFactory.getGson().fromJson(jmsMessageBody, Data.class);
        return new ThingModuleInformMessage[]{
                new ThingModuleInformMessage(
                        data.productId,
                        data.thingId,
                        data.timestamp,
                        data.moduleId,
                        data.version
                )
        };
    }

    private record Data(
            @SerializedName("productKey") String productId,
            @SerializedName("deviceName") String thingId,
            @SerializedName("moduleName") String moduleId,
            @SerializedName("messageCreateTime") long timestamp,
            @SerializedName("moduleVersion") String version
    ) {

    }

}
