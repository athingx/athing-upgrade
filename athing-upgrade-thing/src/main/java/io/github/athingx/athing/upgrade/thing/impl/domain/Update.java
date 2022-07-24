package io.github.athingx.athing.upgrade.thing.impl.domain;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.thing.api.op.OpData;

/**
 * 更新模块版本
 */
public class Update implements OpData {

    @SerializedName("id")
    private final String token;

    @SerializedName("params")
    private final Param param;

    public Update(String token, String moduleId, String version) {
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
