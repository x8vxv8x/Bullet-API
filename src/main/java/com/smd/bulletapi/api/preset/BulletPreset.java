package com.smd.bulletapi.api.preset;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.builder.BulletBuilder;

@SpiApi
@FunctionalInterface
public interface BulletPreset {
    void apply(BulletBuilder builder);
}
