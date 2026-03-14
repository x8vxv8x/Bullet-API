package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.builder.LaserBuilder;
import com.smd.bulletapi.api.handle.LaserHandle;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.common.DanmakuManager;
import net.minecraft.util.math.Vec3d;
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

    public static void remove(World world, int id) {
        DanmakuManager.getInstance().removeLaser(world, id);
    }

    public static boolean exists(World world, int id) {
        return DanmakuManager.getInstance().hasLaser(world, id);
    }

    public static boolean updateTransform(World world, int id, Vec3d start, Vec3d direction) {
        return DanmakuManager.getInstance().updateLaserTransform(world, id, start, direction);
    }

    public static LaserSnapshot snapshot(World world, int id) {
        return DanmakuManager.getInstance().getLaserSnapshot(world, id);
    }
}
