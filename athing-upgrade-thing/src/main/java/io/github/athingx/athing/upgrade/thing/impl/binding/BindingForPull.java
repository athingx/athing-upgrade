package io.github.athingx.athing.upgrade.thing.impl.binding;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpBinding;
import io.github.athingx.athing.thing.api.op.OpCaller;
import io.github.athingx.athing.thing.api.op.OpGroupBind;
import io.github.athingx.athing.thing.api.op.OpReply;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.builder.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeImpl;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeProcessorImpl;
import io.github.athingx.athing.upgrade.thing.impl.domain.Meta;
import io.github.athingx.athing.upgrade.thing.impl.domain.Pull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.function.ThingFn.mappingJsonFromByte;
import static io.github.athingx.athing.thing.api.function.ThingFn.mappingJsonToOpReply;
import static java.nio.charset.StandardCharsets.UTF_8;

public class BindingForPull implements OpBinding<OpCaller<Pull, OpReply<Upgrade>>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final ThingUpgradeOption option;

    public BindingForPull(Thing thing, ThingUpgradeOption option) {
        this.thing = thing;
        this.option = option;
    }

    @Override
    public CompletableFuture<OpCaller<Pull, OpReply<Upgrade>>> binding(OpGroupBind group) {
        return group.bind("/sys/%s/thing/ota/firmware/get_reply".formatted(thing.path().toURN()))
                .map(mappingJsonFromByte(UTF_8))
                .map(mappingJsonToOpReply(Meta.class))
                .call((topic, reply) -> OpReply.reply(
                        reply.token(),
                        reply.code(),
                        reply.desc(),
                        new UpgradeImpl(
                                thing,
                                reply.data(),
                                option,
                                new UpgradeProcessorImpl(thing, reply.data())
                        )
                ));
    }

}
