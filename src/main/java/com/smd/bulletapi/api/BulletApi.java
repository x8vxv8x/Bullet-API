package com.smd.bulletapi.api;

import com.smd.bulletapi.api.builder.BulletBuilder;
import com.smd.bulletapi.common.DanmakuManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class BulletApi {
    private BulletApi() {}

    public static BulletBuilder builder(World world) {
        return new BulletBuilder(world);
    }

    public static void remove(World world, int id) {
        DanmakuManager.getInstance().removeBullet(world, id);
    }

    public static void updateVelocity(World world, int id, Vec3d newVelocity) {
        DanmakuManager.getInstance().updateBulletVelocity(world, id, newVelocity);
    }
}
