package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.common.DanmakuManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@PublicApi
public final class BulletHandle {
    private final World world;
    private final int id;

    public BulletHandle(World world, int id) {
        this.world = world;
        this.id = id;
    }

    public World getWorld() { return world; }
    public int getId() { return id; }
    public boolean exists() { return DanmakuManager.getInstance().hasBullet(world, id); }
    public void remove() { DanmakuManager.getInstance().removeBullet(world, id); }
    public void setPosition(Vec3d position) { DanmakuManager.getInstance().updateBulletPosition(world, id, position); }
    public void setVelocity(Vec3d velocity) { DanmakuManager.getInstance().updateBulletVelocity(world, id, velocity); }
    public void setMotion(Vec3d position, Vec3d velocity) { DanmakuManager.getInstance().updateBulletMotion(world, id, position, velocity); }
    public void setLife(int life) { DanmakuManager.getInstance().updateBulletLife(world, id, life); }
    public BulletSnapshot snapshot() { return DanmakuManager.getInstance().getBulletSnapshot(world, id); }
}
