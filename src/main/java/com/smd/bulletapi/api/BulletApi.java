package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.builder.BulletBuilder;
import com.smd.bulletapi.api.handle.BulletHandle;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.common.DanmakuManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@PublicApi
public final class BulletApi {
    private BulletApi() {}

    public static BulletBuilder builder(World world) {
        return new BulletBuilder(world);
    }

    public static BulletHandle handle(World world, int id) {
        return new BulletHandle(world, id);
    }

    public static void remove(World world, int id) {
        DanmakuManager.getInstance().removeBullet(world, id);
    }

    public static void updateVelocity(World world, int id, Vec3d newVelocity) {
        DanmakuManager.getInstance().updateBulletVelocity(world, id, newVelocity);
    }

    public static boolean exists(World world, int id) {
        return DanmakuManager.getInstance().hasBullet(world, id);
    }

    public static BulletSnapshot snapshot(World world, int id) {
        return DanmakuManager.getInstance().getBulletSnapshot(world, id);
    }
}
