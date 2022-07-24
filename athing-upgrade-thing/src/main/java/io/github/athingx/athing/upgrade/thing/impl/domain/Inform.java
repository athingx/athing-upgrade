package io.github.athingx.athing.upgrade.thing.impl.domain;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.thing.api.op.OpData;

/**
 * 模块通告
 */
public class Inform implements OpData {

    @SerializedName("id")
    private final String token;

    @SerializedName("params")
    private final Param param;

    /**
     * 模块通告
     *
     * @param token    令牌
     * @param moduleId 模块ID
     * @param version  模块版本
     */
    public Inform(String token, String moduleId, String version) {
        this.token = token;
        this.param = new Param(moduleId, version);
    }

    @Override
    public String token() {
        return token;
    }

    private record Param(
            @SerializedName("module") String moduleId,
            @SerializedName("version") String version
    ) {

    }

}
