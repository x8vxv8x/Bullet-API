package com.smd.bulletapi.api;

import com.smd.bulletapi.api.builder.LaserBuilder;
import com.smd.bulletapi.common.DanmakuManager;
import net.minecraft.world.World;

public final class LaserApi {
    private LaserApi() {}

    public static LaserBuilder builder(World world) {
        return new LaserBuilder(world);
    }

    public static void remove(World world, int id) {
        DanmakuManager.getInstance().removeLaser(world, id);
    }
}
