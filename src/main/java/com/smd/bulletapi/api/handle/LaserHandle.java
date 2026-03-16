package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.common.DanmakuManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@PublicApi
public final class LaserHandle {
    private final World world;
    private final int id;

    public LaserHandle(World world, int id) {
        this.world = world;
        this.id = id;
    }

    public World getWorld() { return world; }
    public int getId() { return id; }
    public boolean exists() { return DanmakuManager.getInstance().hasLaser(world, id); }
    public void remove() { DanmakuManager.getInstance().removeLaser(world, id); }
    public boolean setTransform(Vec3d start, Vec3d direction) { return DanmakuManager.getInstance().updateLaserTransform(world, id, start, direction); }
    public boolean setLength(double length) { return DanmakuManager.getInstance().updateLaserLength(world, id, length); }
    public void setLife(int life) { DanmakuManager.getInstance().updateLaserLife(world, id, life); }
    public LaserSnapshot snapshot() { return DanmakuManager.getInstance().getLaserSnapshot(world, id); }
}
