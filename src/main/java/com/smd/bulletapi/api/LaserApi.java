package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.builder.LaserBuilder;
import com.smd.bulletapi.api.handle.LaserHandle;
import net.minecraft.world.World;

@PublicApi
public final class LaserApi {
    private LaserApi() {}

    public static LaserBuilder builder(World world) {
        return new LaserBuilder(world);
    }

    public static LaserHandle handle(World world, int id) {
        return new LaserHandle(world, id);
    }
}
