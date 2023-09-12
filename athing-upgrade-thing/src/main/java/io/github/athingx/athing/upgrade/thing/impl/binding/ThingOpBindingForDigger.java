package io.github.athingx.athing.upgrade.thing.impl.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.athingx.athing.common.ThingCodes;
import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.*;
import io.github.athingx.athing.thing.api.op.function.OpFunction;
import io.github.athingx.athing.thing.api.util.MapData;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.Digger;
import io.github.athingx.athing.upgrade.thing.impl.util.SignUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

public class ThingOpBindingForDigger implements ThingOpBinding<Digger> {

    private final ThingUpgradeOption option;

    public ThingOpBindingForDigger(ThingUpgradeOption option) {
        this.option = option;
    }

    @Override
    public CompletableFuture<Digger> bind(Thing thing) {
        final var reTopic = "/sys/%s/thing/file/download_reply".formatted(thing.path().toURN());
        final var topic = "/sys/%s/thing/file/download".formatted(thing.path().toURN());
        final var opOption = new ThingOpBind.Option()
                .setTimeoutMs(option.getDownloadTimeoutMs());
        return thing.op().bind(reTopic)
                .map(mappingBytesToResponseOpReply())
                .caller(opOption, OpFunction.identity())
                .thenApply(caller -> caller
                        .route(v -> topic)
                        .compose(mappingRequestToOpData(thing.op().genToken()))
                        .then(OpReply::handle)
                )
                .thenApply(caller -> new Digger() {
                    @Override
                    public CompletableFuture<Response> download(Request request) {
                        return caller.call(request);
                    }

                    @Override
                    public CompletableFuture<Void> unbind() {
                        return caller.unbind();
                    }
                });
    }

    private static Function<Digger.Request, OpData> mappingRequestToOpData(String token) {
        return request -> new OpMapData(token, new MapData()
                .putProperty("id", token)
                .putProperty("version", "1.0")
                .putProperty("params", prop -> prop
                        .putProperty("fileInfo", infoP -> infoP
                                .putProperty("streamId", request.streamId())
                                .putProperty("fileId", request.fileId())
                        )
                        .putProperty("fileBlock", blockP -> blockP
                                .putProperty("size", request.size())
                                .putProperty("offset", request.position())
                        )
                )
        );
    }

    private static OpFunction<byte[], OpReply<Digger.Response>> mappingBytesToResponseOpReply() {

        record Block(long fileLength, long bOffset, int bSize) {
        }

        return (topic, data) -> {

            // 读取Json长度


            // 读取Json
            final var jsonOffset = 2;
            final var jsonLength = (short) ((data[0] << 8) | data[1]);
            final var json = new String(data, jsonOffset, jsonLength, UTF_8);

            // 解析响应
            final var root = JsonParser.parseString(json).getAsJsonObject();
            final var reply = new OpReply<Block>(
                    ofNullable(root.get("id")).map(JsonElement::getAsString).orElseThrow(() -> new IllegalArgumentException("missing id")),
                    ofNullable(root.get("code")).map(JsonElement::getAsInt).orElseThrow(() -> new IllegalArgumentException("missing code")),
                    ofNullable(root.get("message")).map(JsonElement::getAsString).orElse(null),
                    ofNullable(root.get("data"))
                            .filter(element -> !element.getAsJsonObject().keySet().isEmpty())
                            .map(element -> {
                                final var dataJson = element.getAsJsonObject();
                                return new Block(
                                        ofNullable(dataJson.get("fileLength")).map(JsonElement::getAsLong).orElseThrow(() -> new IllegalArgumentException("missing data.fileLength")),
                                        ofNullable(dataJson.get("bOffset")).map(JsonElement::getAsLong).orElseThrow(() -> new IllegalArgumentException("missing data.bOffset")),
                                        ofNullable(dataJson.get("bSize")).map(JsonElement::getAsInt).orElseThrow(() -> new IllegalArgumentException("missing data.bSize"))
                                );
                            })
                            .orElse(null)
            );

            // 读取数据
            final var blockOffset = jsonOffset + jsonLength;
            final var blockLength = reply.data().bSize();

            // 读取CRC校验位
            final var signOffset = blockOffset + blockLength;
            final var sign = (short) ((data[signOffset] << 8) | data[signOffset + 1]);

            // 校验CRC
            if (sign != SignUtils.crc16_ibm(data, blockOffset, blockLength)) {
                return OpReply.fail(reply.token(), ThingCodes.REQUEST_ERROR, "block crc error!");
            }

            // 转换为
            return new OpReply<>(reply.token(), reply.code(), reply.desc(),
                    new Digger.Response(
                            reply.data().fileLength(),
                            reply.data().bOffset(),
                            ByteBuffer.wrap(data, blockOffset, blockLength)
                    ));
        };
    }

}