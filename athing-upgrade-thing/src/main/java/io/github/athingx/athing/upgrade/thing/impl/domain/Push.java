package io.github.athingx.athing.upgrade.thing.impl.domain;

import com.google.gson.annotations.SerializedName;

public record Push(
        @SerializedName("id") String token,
        @SerializedName("data") Meta meta
) {

}
