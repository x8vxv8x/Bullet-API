package com.smd.bulletapi.api.preset;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.builder.LaserBuilder;

@SpiApi
@FunctionalInterface
public interface LaserPreset {
    void apply(LaserBuilder builder);
}
