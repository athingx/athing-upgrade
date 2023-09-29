package io.github.athingx.athing.upgrade.thing.impl.binding;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.athingx.athing.common.ThingCodes;
import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.*;
import io.github.athingx.athing.thing.api.util.MapData;
import io.github.athingx.athing.upgrade.thing.impl.Digger;
import io.github.athingx.athing.upgrade.thing.impl.util.SignUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.op.Encoder.encodeJsonToBytes;
import static io.github.athingx.athing.thing.api.op.Encoder.encodeOpRequestToJson;
import static io.github.athingx.athing.upgrade.thing.impl.util.JsonObjectUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class OpBindingForDigger implements OpBinding<Digger> {

    @Override
    public CompletableFuture<Digger> bind(Thing thing) {
        return thing.op()
                .codec(codecBytesToDigger())
                .caller("/sys/%s/thing/file/download_reply".formatted(thing.path().toURN()), Codec.none())
                .thenApply(caller -> caller
                        .topics("/sys/%s/thing/file/download".formatted(thing.path().toURN()))
                        .<Digger.Request>compose(request -> new OpRequest<>(
                                thing.op().genToken(),
                                new MapData()
                                        .putProperty("fileInfo", infoP -> infoP
                                                .putProperty("streamId", request.streamId())
                                                .putProperty("fileId", request.fileId())
                                        )
                                        .putProperty("fileBlock", blockP -> blockP
                                                .putProperty("size", request.size())
                                                .putProperty("offset", request.position())
                                        )
                        ))
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

    private static Codec<byte[], byte[], OpRequest<MapData>, OpReply<Digger.Response>> codecBytesToDigger() {
        return new Codec<>() {

            @Override
            public Encoder<OpRequest<MapData>, byte[]> encoder() {
                return encodeJsonToBytes(UTF_8)
                        .compose(encodeOpRequestToJson(MapData.class));

            }

            @Override
            public Decoder<byte[], OpReply<Digger.Response>> decoder() {
                return new Decoder<>() {

                    record Block(long fileLength, long bOffset, int bSize) {

                        static Block toBlock(JsonObject dataJson) {
                            return new Block(
                                    requireAsLong(dataJson, "fileLength"),
                                    requireAsLong(dataJson, "bOffset"),
                                    requireAsInt(dataJson, "bSize")
                            );
                        }

                    }

                    @Override
                    public OpReply<Digger.Response> decode(String topic, byte[] data) {
                        // 读取Json
                        final var jsonOffset = 2;
                        final var jsonLength = (short) ((data[0] << 8) | data[1]);
                        final var json = new String(data, jsonOffset, jsonLength, UTF_8);

                        // 解析响应
                        final var root = JsonParser.parseString(json).getAsJsonObject();
                        final var reply = new OpReply<Block>(
                                requireAsString(root, "id"),
                                requireAsInt(root, "code"),
                                getAsString(root, "message"),
                                isNotEmptyProperty(root, "data")
                                        ? Block.toBlock(root.get("data").getAsJsonObject())
                                        : null
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
                    }
                };
            }

        };
    }

}
