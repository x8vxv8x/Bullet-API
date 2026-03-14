package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
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
    public boolean exists() { return BulletApi.exists(world, id); }
    public void remove() { BulletApi.remove(world, id); }
    public void updateVelocity(Vec3d velocity) { BulletApi.updateVelocity(world, id, velocity); }
    public BulletSnapshot snapshot() { return BulletApi.snapshot(world, id); }
}
