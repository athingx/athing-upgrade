package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpMapData;
import io.github.athingx.athing.thing.api.util.MapData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 升级处理器实现
 */
public class ProcessorImpl implements Processor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final String module;
    private final AtomicReference<Report> reportRef = new AtomicReference<>();


    public ProcessorImpl(Thing thing, String module) {
        this.thing = thing;
        this.module = module;
    }

    // 判断本次是否需要上报
    private static boolean isNeedReport(Report expect, Report latest) {

        // 如果期待的进度为空，则需要上报
        if (null == expect) {
            return true;
        }

        // 如果上一次上报的进度为负数，则后续均不需要上报
        if (expect.step < 0) {
            return false;
        }

        // 如果当前需要上报的进度为负数，说明当前是异常，需要立即上报
        if (latest.step < 0) {
            return true;
        }

        // 如果当前需要上报的进度大于上一次上报的进度，而且两者之间间隔超过3秒，则需要上报
        return latest.step > expect.step
                && latest.timestamp - expect.timestamp > 3000;
    }

    /**
     * 上报信息
     *
     * @param step      进度
     * @param timestamp 时间戳
     */
    private record Report(int step, long timestamp) {

    }

    @Override
    public CompletableFuture<Void> processing(int step, String desc) {

        // 检查是否需要上报进度，如果不需要则直接返回， 如果需要则进行上报且记录本次上报信息
        while (true) {
            final var expect = reportRef.get();
            final var latest = new Report(step, System.currentTimeMillis());
            if (isNeedReport(expect, latest)) {
                if (reportRef.compareAndSet(expect, latest)) {
                    break;
                }
            } else {
                return completedFuture(null);
            }
        }

        // 上报进度
        final var token = thing.op().genToken();
        return thing.op()
                .post("/ota/device/progress/%s/".formatted(thing.path().toURN()), new OpMapData(token, new MapData()
                        .putProperty("id", token)
                        .putProperty("params", prop -> prop
                                .putProperty("module", module)
                                .putProperty("step", step)
                                .putProperty("desc", desc)
                        )))
                .whenComplete((v, cause) -> logger.debug("{}/upgrade/processing completed! token={};step={};desc={};",
                        thing.path(),
                        token,
                        step,
                        desc,
                        cause
                ));

    }

}
