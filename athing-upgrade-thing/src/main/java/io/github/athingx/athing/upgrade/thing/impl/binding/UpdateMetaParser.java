package io.github.athingx.athing.upgrade.thing.impl.binding;

import com.google.gson.JsonObject;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static io.github.athingx.athing.upgrade.thing.impl.util.JsonObjectUtils.*;

class UpdateMetaParser {

    private static final Parser parseForSignHttp = (json, trigger) -> {

        // 如果是mqtt升级或者多文件升级则返回空
        if (isMqtt(json) || isMulti(json)) {
            return null;
        }

        final var module = requireAsString(json, "module");
        final var version = requireAsString(json, "version");
        final var isDiff = getAsLong(json, "isDiff") == 1L;

        // 提取下载文件元数据
        final var stores = new HashSet<UpgradeMeta.StoreMeta>() {{
            add(new UpgradeMeta.StoreMeta(
                    "%s-%s.data".formatted(module, version),
                    requireAsUri(json, "url"),
                    requireAsLong(json, "size"),
                    new UpgradeMeta.StoreMeta.Info(module, version),
                    new UpgradeMeta.StoreMeta.Sign(
                            requireAsString(json, "signMethod"), requireAsString(json, "sign")
                    )
            ));
        }};

        // 返回升级元数据
        return new UpgradeMeta(module, version, isDiff, trigger, parseExtra(json), stores);
    };

    private static final Parser parseForSignMqtt = (json, trigger) -> {

        // 如果不是mqtt升级或者多文件升级则返回空
        if (!isMqtt(json) || isMulti(json)) {
            return null;
        }

        final var module = requireAsString(json, "module");
        final var version = requireAsString(json, "version");
        final var isDiff = getAsLong(json, "isDiff") == 1L;
        final var streamId = requireAsString(json, "streamId");
        final var fileId = requireAsString(json, "streamFileId");

        // 提取下载文件元数据
        final var stores = new HashSet<UpgradeMeta.StoreMeta>() {{
            add(new UpgradeMeta.StoreMeta(
                    "%s-%s.data".formatted(module, version),
                    URI.create("mqtt://%s/%s".formatted(streamId, fileId)),
                    requireAsLong(json, "size"),
                    new UpgradeMeta.StoreMeta.Info(module, version),
                    new UpgradeMeta.StoreMeta.Sign(
                            requireAsString(json, "signMethod"), requireAsString(json, "sign")
                    )
            ));
        }};

        // 返回升级元数据
        return new UpgradeMeta(module, version, isDiff, trigger, parseExtra(json), stores);
    };

    private static final Parser parseForMultiHttp = (json, trigger) -> {

        if (isMqtt(json) || !isMulti(json)) {
            return null;
        }

        final var module = requireAsString(json, "module");
        final var version = requireAsString(json, "version");
        final var isDiff = getAsLong(json, "isDiff") == 1L;

        // 提取下载文件元数据
        final var stores = new HashSet<UpgradeMeta.StoreMeta>() {{
            json.get("files").getAsJsonArray().forEach(file -> {
                final var fileJson = file.getAsJsonObject();
                add(new UpgradeMeta.StoreMeta(
                        requireAsString(fileJson, "fileName"),
                        requireAsUri(fileJson, "fileUrl"),
                        requireAsLong(fileJson, "fileSize"),
                        new UpgradeMeta.StoreMeta.Info(module, version),
                        new UpgradeMeta.StoreMeta.Sign(
                                requireAsString(json, "signMethod"), requireAsString(fileJson, "fileSign")
                        )
                ));
            });
        }};

        // 返回升级元数据
        return new UpgradeMeta(module, version, isDiff, trigger, parseExtra(json), stores);
    };

    private static final Parser[] parsers = new Parser[]{
            parseForSignHttp,
            parseForSignMqtt,
            parseForMultiHttp
    };

    private static boolean isMqtt(JsonObject json) {
        return json.has("dProtocol") && json.get("dProtocol").getAsString().equals("mqtt");
    }

    private static boolean isMulti(JsonObject json) {
        return json.has("files");
    }

    private static Map<String, String> parseExtra(JsonObject json) {
        final var map = new HashMap<String, String>();
        if (json.has("extData")) {
            json.get("extData").getAsJsonObject().entrySet().forEach(entry ->
                    map.put(entry.getKey(), entry.getValue().getAsString()));
        }
        return Collections.unmodifiableMap(map);
    }

    public static UpgradeMeta parseUpgradeMeta(JsonObject json, Upgrade.Trigger trigger) {
        for (final var parser : parsers) {
            final var meta = parser.parse(json, trigger);
            if (null != meta) {
                return meta;
            }
        }
        throw new IllegalArgumentException("unknown upgrade meta!");
    }

    private interface Parser {
        UpgradeMeta parse(JsonObject json, Upgrade.Trigger trigger);
    }

}
