package io.github.athingx.athing.upgrade.thing.impl.binder;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpBind;
import io.github.athingx.athing.thing.api.op.OpGroupBinder;
import io.github.athingx.athing.thing.api.op.OpGroupBinding;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import io.github.athingx.athing.upgrade.thing.builder.ThingUpgradeOption;
import io.github.athingx.athing.upgrade.thing.impl.Informer;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeImpl;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeProcessorImpl;
import io.github.athingx.athing.upgrade.thing.impl.domain.Push;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.function.CompletableFutureFn.whenCompleted;
import static io.github.athingx.athing.thing.api.function.ThingFn.mappingJsonFromByte;
import static io.github.athingx.athing.thing.api.function.ThingFn.mappingJsonToType;
import static io.github.athingx.athing.upgrade.thing.impl.UpgradeProcessor.Step.STEP_UPGRADES_FAILURE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PushOpBinder implements OpGroupBinder<OpBind> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final ThingUpgradeOption option;
    private final Set<UpgradeListener> listeners;
    private final Informer informer;

    public PushOpBinder(Thing thing, ThingUpgradeOption option, Set<UpgradeListener> listeners, Informer informer) {
        this.thing = thing;
        this.option = option;
        this.listeners = listeners;
        this.informer = informer;
    }

    @Override
    public CompletableFuture<OpBind> bindFor(OpGroupBinding group) {
        return group.binding("/ota/device/upgrade/%s".formatted(thing.path().toURN()))
                .map(mappingJsonFromByte(UTF_8))
                .map(mappingJsonToType(Push.class))
                .bind((topic, push) -> {
                    final var meta = push.meta();
                    final var process = new UpgradeProcessorImpl(thing, meta);
                    final var upgrade = new UpgradeImpl(thing, meta, option, process);

                    // receive
                    logger.debug("{}/upgrade/push received, push={};module={};version={};",
                            thing.path(),
                            push.token(),
                            meta.moduleId(),
                            meta.version()
                    );

                    // 如果没配置监听器，则返回升级失败
                    if (listeners.isEmpty()) {
                        logger.warn("{}/upgrade/push give up: none-listener, push={};module={};version={};",
                                thing.path(),
                                push.token(),
                                meta.moduleId(),
                                meta.version()
                        );
                        process.processing(STEP_UPGRADES_FAILURE, "none-listener");
                        return;
                    }

                    // apply
                    try {
                        listeners.forEach(listener -> listener.apply(upgrade));
                        logger.debug("{}/upgrade/push apply success, push={};module={};version={};",
                                thing.path(),
                                push.token(),
                                meta.moduleId(),
                                meta.version()
                        );
                    } catch (Throwable cause) {
                        logger.warn("{}/upgrade/push apply failure, push={};module={};version={};",
                                thing.path(),
                                push.token(),
                                meta.moduleId(),
                                meta.version(),
                                cause
                        );
                        process.processing(STEP_UPGRADES_FAILURE, cause.getLocalizedMessage());
                    }

                    // commit
                    informer.inform(meta.moduleId(), meta.version())
                            .whenComplete(whenCompleted(
                                    (v) -> logger.debug("{}/update/push commit success, push={};module={};version={};",
                                            thing.path(), push.token(), meta.moduleId(), meta.version()),
                                    (ex) -> logger.warn("{}/update/push commit failure, push={};module={};version={};",
                                            thing.path(), push.token(), meta.moduleId(), meta.version(), ex)
                            ));

                });
    }

}
