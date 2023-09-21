package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.op.ThingOpBinder;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * MQTT下载器
 */
public interface Digger extends ThingOpBinder {

    /**
     * 下载
     *
     * @param request 请求
     * @return 响应
     */
    CompletableFuture<Response> download(Request request);

    /**
     * MQTT下载请求
     *
     * @param streamId 流ID
     * @param fileId   文件ID
     * @param total    总字节数
     * @param position 本次下载位置
     * @param size     本次下载大小
     */
    record Request(String streamId, String fileId, long total, long position, int size) {

    }

    /**
     * MQTT下载响应
     *
     * @param total    总字节数
     * @param position 本次下载位置
     * @param buffer   本次下载数据
     */
    record Response(long total, long position, ByteBuffer buffer) {

    }

}
