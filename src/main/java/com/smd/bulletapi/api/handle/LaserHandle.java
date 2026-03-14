package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.LaserApi;
import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
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
    public boolean exists() { return LaserApi.exists(world, id); }
    public void remove() { LaserApi.remove(world, id); }
    public boolean updateTransform(Vec3d start, Vec3d direction) { return LaserApi.updateTransform(world, id, start, direction); }
    public LaserSnapshot snapshot() { return LaserApi.snapshot(world, id); }
}
