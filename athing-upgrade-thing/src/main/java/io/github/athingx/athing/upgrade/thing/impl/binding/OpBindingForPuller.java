package io.github.athingx.athing.upgrade.thing.impl.binding;

import com.google.gson.JsonParser;
import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.*;
import io.github.athingx.athing.thing.api.util.MapData;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.impl.Puller;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;

import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.op.Codec.codecBytesToJson;
import static io.github.athingx.athing.upgrade.thing.impl.binding.UpdateMetaParser.parseUpgradeMeta;
import static io.github.athingx.athing.upgrade.thing.impl.util.JsonObjectUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class OpBindingForPuller implements OpBinding<Puller> {

    @Override
    public CompletableFuture<Puller> bind(Thing thing) {
        return thing.op()
                .codec(codecBytesToJson(UTF_8))
                .codec(codecJsonToPuller())
                .caller("/sys/%s/thing/ota/firmware/get_reply".formatted(thing.path().toURN()), Codec.none())
                .thenApply(caller -> caller
                        .topics("/sys/%s/thing/ota/firmware/get".formatted(thing.path().toURN()))
                        .<String>compose(module -> new OpRequest<>(
                                thing.op().genToken(),
                                "thing.ota.firmware.get",
                                new MapData().putProperty("module", module)
                        ))
                        .then(OpReply::handle)
                )
                .thenApply(caller -> new Puller() {

                    @Override
                    public CompletableFuture<UpgradeMeta> pull(String module) {
                        return caller.call(module);
                    }

                    @Override
                    public CompletableFuture<Void> unbind() {
                        return caller.unbind();
                    }

                });
    }

    private static Codec<String, String, OpRequest<MapData>, OpReply<UpgradeMeta>> codecJsonToPuller() {
        return new Codec<>() {

            @Override
            public Encoder<OpRequest<MapData>, String> encoder() {
                return Encoder.encodeOpRequestToJson(MapData.class);
            }

            @Override
            public Decoder<String, OpReply<UpgradeMeta>> decoder() {
                return (topic, json) -> {
                    final var root = JsonParser.parseString(json).getAsJsonObject();
                    return new OpReply<>(
                            requireAsString(root, "id"),
                            requireAsInt(root, "code"),
                            getAsString(root, "message"),
                            isNotEmptyProperty(root, "data")
                                    ? parseUpgradeMeta(root.get("data").getAsJsonObject(), Upgrade.Trigger.PULL)
                                    : null
                    );
                };
            }
        };
    }

}
