package io.github.athingx.athing.upgrade.thing.impl.domain;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.thing.api.op.OpData;

/**
 * 升级进度
 */
public class Process implements OpData {

    @SerializedName("id")
    private final String token;

    @SerializedName("params")
    private final Param param;

    public Process(String token, String moduleId, int step, String desc) {
        this.token = token;
        this.param = new Param(moduleId, step, desc);
    }

    @Override
    public String token() {
        return token;
    }


    private record Param(
            @SerializedName("module") String moduleId,
            @SerializedName("step") int step,
            @SerializedName("desc") String desc
    ) {

    }

}
