package io.github.athingx.athing.upgrade.thing.impl.binding;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.ThingOpBinding;
import io.github.athingx.athing.thing.api.op.function.OpFunction;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.impl.Pusher;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.op.function.OpMapper.mappingBytesToJson;
import static io.github.athingx.athing.upgrade.thing.impl.binding.UpdateMetaParser.parseUpgradeMeta;
import static io.github.athingx.athing.upgrade.thing.impl.util.JsonObjectUtils.requireAsString;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ThingOpBindingForPusher implements ThingOpBinding<Pusher> {

    private final BlockingQueue<UpgradeMeta> queue;

    public ThingOpBindingForPusher(BlockingQueue<UpgradeMeta> queue) {
        this.queue = queue;
    }

    @Override
    public CompletableFuture<Pusher> bind(Thing thing) {
        return thing.op().bind("/ota/device/upgrade/%s".formatted(thing.path().toURN()))
                .map(mappingBytesToJson(UTF_8))
                .map(mappingJsonToJsonObject())
                .filter((topic, root) -> Objects.equals("1000", requireAsString(root, "checksum")))
                .map((topic, root) -> root.get("data").getAsJsonObject())
                .map((topic, data) -> parseUpgradeMeta(data, Upgrade.Trigger.PUSH))
                .consumer((topic, meta) -> {
                    if (!queue.offer(meta)) {
                        throw new IllegalStateException("queue overflow!");
                    }
                })
                .thenApply(binder -> binder::unbind);
    }

    private static OpFunction<String, JsonObject> mappingJsonToJsonObject() {
        return (topic, json) -> JsonParser.parseString(json).getAsJsonObject();
    }

}
