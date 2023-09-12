package io.github.athingx.athing.upgrade.thing.impl.binding;

import com.google.gson.JsonParser;
import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpReply;
import io.github.athingx.athing.thing.api.op.OpRequest;
import io.github.athingx.athing.thing.api.op.ThingOpBinding;
import io.github.athingx.athing.thing.api.op.function.OpFunction;
import io.github.athingx.athing.thing.api.util.MapData;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.impl.Puller;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;

import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.op.function.OpMapper.mappingBytesToJson;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ThingOpBindingForPuller implements ThingOpBinding<Puller> {

    @Override
    public CompletableFuture<Puller> bind(Thing thing) {
        final var reTopic = "/sys/%s/thing/ota/firmware/get_reply".formatted(thing.path().toURN());
        final var topic = "/sys/%s/thing/ota/firmware/get".formatted(thing.path().toURN());
        return thing.op().bind(reTopic)
                .map(mappingBytesToJson(UTF_8))
                .map(mappingJsonToUpgradeOpReply())
                .caller(OpFunction.identity())
                .thenApply(caller -> caller
                        .route(v -> topic)
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

    private static OpFunction<String, OpReply<UpgradeMeta>> mappingJsonToUpgradeOpReply() {
        return (topic, json) -> {
            final var root = JsonParser.parseString(json).getAsJsonObject();
            return new OpReply<>(
                    root.get("id").getAsString(),
                    root.get("code").getAsInt(),
                    root.has("message")
                            ? root.get("message").getAsString()
                            : null,
                    root.has("data") && !root.get("data").getAsJsonObject().keySet().isEmpty()
                            ? UpdateMetaParser.parseUpgradeMeta(root.get("data").getAsJsonObject(), Upgrade.Trigger.PULL)
                            : null
            );
        };
    }

}
