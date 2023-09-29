package io.github.athingx.athing.upgrade.thing.impl.binding;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpBinding;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.impl.Pusher;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static com.google.gson.JsonParser.parseString;
import static io.github.athingx.athing.thing.api.op.Decoder.decodeBytesToJson;
import static io.github.athingx.athing.thing.api.op.Decoder.filter;
import static io.github.athingx.athing.upgrade.thing.impl.binding.UpdateMetaParser.parseUpgradeMeta;
import static io.github.athingx.athing.upgrade.thing.impl.util.JsonObjectUtils.requireAsString;
import static java.nio.charset.StandardCharsets.UTF_8;

public class OpBindingForPusher implements OpBinding<Pusher> {

    private final BlockingQueue<UpgradeMeta> queue;

    public OpBindingForPusher(BlockingQueue<UpgradeMeta> queue) {
        this.queue = queue;
    }

    @Override
    public CompletableFuture<Pusher> bind(Thing thing) {

        return thing.op()
                .decode(decodeBytesToJson(UTF_8))
                .decode((topic, json) -> parseString(json).getAsJsonObject())
                .decode(filter((topic, root) -> "1000".equals(requireAsString(root, "code"))))
                .decode((topic, root) -> root.get("data").getAsJsonObject())
                .decode((topic, data) -> parseUpgradeMeta(data, Upgrade.Trigger.PUSH))
                .consumer("/ota/device/upgrade/%s".formatted(thing.path().toURN()), (topic, meta) -> {
                    if (!queue.offer(meta)) {
                        throw new IllegalStateException("queue overflow!");
                    }
                })
                .thenApply(binder -> binder::unbind);

    }

}
